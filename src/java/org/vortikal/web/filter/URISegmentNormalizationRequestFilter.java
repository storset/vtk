package org.vortikal.web.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A request filter that normalizes the URI of an <code>HttpServletRequest</code>
 * by removing dot segments ('.' or '..').
 * 
 * <p>Configurable properties:
 * <ul>
 *  <li><code>normalizeSlashes</code> - Multiple consecutive slashes 
 *  (e.g. '/foo///bar///too//many//slashes') are, by default, not normalized 
 *  into a single slash. To also enable this kind of normalization, set this
 *  property to <code>true</code>.
 *  </li>
 * </ul>
 * </p>
 * 
 * @author ovyiste
 *
 */
public class URISegmentNormalizationRequestFilter implements RequestFilter {

    Log logger = LogFactory.getLog(URISegmentNormalizationRequestFilter.class);
    
    private int order = Integer.MAX_VALUE;
    private boolean normalizeSlashes = false;
    
    public HttpServletRequest filterRequest(HttpServletRequest request) {
        return new URISegmentNormalizationRequestWrapper(request);
    }

    private class URISegmentNormalizationRequestWrapper extends HttpServletRequestWrapper {
        
        private String normalizedURI;
        
        public String getRequestURI() {
            return this.normalizedURI;
        }
        
        public URISegmentNormalizationRequestWrapper(HttpServletRequest request) {
            super(request);
            
            this.normalizedURI = removeDotSegments(request.getRequestURI());

            if (normalizeSlashes) {
                this.normalizedURI = this.normalizedURI.replaceAll("[/]+", "/");
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Request URI: '" + request.getRequestURI() + 
                        "', normalized URI: '" + this.normalizedURI + "'");
            }
        }

        /**
         * This method should normalize most combinations of dots and slashes in
         * URIs.
         * TODO: Put in <code>org.vortikal.util.web.URLUtil</code> ?
         * 
         * @param uri The URI to be normalized.
         * @return The normalized URI. 
         */
        private String removeDotSegments(String uri) {
            StringBuffer input = new StringBuffer(uri);
            StringBuffer output = new StringBuffer();

            while (input.length() > 0) {
                if (input.length() >= 3 && "../".equals(input.substring(0,3))) {
                    input.delete(0,3);
                } else if (input.length() >= 2 && "./".equals(input.substring(0,2))) {
                    input.delete(0,2);
                } else if (input.length() >= 3 && "/./".equals(input.substring(0,3))) {
                    input.replace(0,3, "/");
                } else if (input.length() == 2 && "/.".equals(input.substring(0,2))) {
                    input.replace(0,2, "/");
                } else if (input.length() >= 4 && "/../".equals(input.substring(0,4))) {
                    input.replace(0,4, "/");
                    removeLastSegment(output);
                } else if (input.length() >= 3 && "/..".equals(input.substring(0,3))) {
                    if (input.length() > 3 && input.charAt(3) != '/') {
                        moveNextSegment(input, output);
                    } else {
                        input.replace(0,3, "/");
                        removeLastSegment(output);
                    }
                } else if (".".equals(input.toString()) || "..".equals(input.toString())) {
                    break;
                } else {
                    moveNextSegment(input, output);
                }
            }
            
            return output.toString();
        }

        private void moveNextSegment(StringBuffer input, StringBuffer output) {
            int next = input.indexOf("/", 1);
            if (next != -1) {
                output.append(input.substring(0, next));
                input.delete(0, next);
            } else {
                output.append(input.toString());
                input.delete(0, input.length());
            }
        }
        
        private void removeLastSegment(StringBuffer uri) {
            int last = uri.lastIndexOf("/");
            if (last != -1) {
                uri.delete(last, uri.length());
            } else {
                uri.delete(0, uri.length());
            }
        }  
        
    }

    public int getOrder() {
        return this.order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isNormalizeSlashes() {
        return normalizeSlashes;
    }

    public void setNormalizeSlashes(boolean normalizeSlashes) {
        this.normalizeSlashes = normalizeSlashes;
    }

}
