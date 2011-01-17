/* Copyright (c) 2009, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.service.URL;

import com.sun.syndication.feed.synd.SyndEntry;

public abstract class AbstractFeedComponent extends ViewRenderingDecoratorComponent {

    protected static final String PARAMETER_ITEM_PICTURE = "item-picture";
    protected static final String PARAMETER_ITEM_PICTURE_DESC = "Must be set to 'true' to show item picture";

    protected static final String PARAMETER_IF_EMPTY_MESSAGE = "if-empty-message";
    protected static final String PARAMETER_IF_EMPTY_MESSAGE_DESC = "Message to be displayd if feed is empty";

    protected static final String PARAMETER_FEED_ELEMENT_ORDER = "element-order";
    protected static final String PARAMETER_FEED_ELEMENT_ORDER_DESC = "The order that the elementes are listed";

    private HtmlPageParser parser = new HtmlPageParser();
    private HtmlPageFilter imgHtmlFilter;
    private HtmlPageFilter noImgHtmlFilter;
    private List<String> defaultElementOrder;

    boolean prameterHasValue(String param, String includeParamValue, DecoratorRequest request) {
        String itemDescriptionString = request.getStringParameter(param);
        if (itemDescriptionString != null && includeParamValue.equalsIgnoreCase(itemDescriptionString)) {
            return true;
        }
        return false;
    }
 
    protected HtmlFragment filterEntry(SyndEntry entry, HtmlPageFilter filter) throws Exception {
        String htmlFragment = null;
        if (entry.getDescription() == null) {
            return null;
        }
        if (entry.getDescription() != null) {
            htmlFragment = entry.getDescription().getValue();
        }
        HtmlFragment fragment = this.parser.parseFragment(htmlFragment);
        fragment.filter(filter);
        return fragment;
    }

    /**
     * Filter: keeps a reference to the first <img> tag, possibly making 
     * the 'src' attribute relative to the supplied base URL. 
     * Invokes another filter to do content selection.
     */
    protected class Filter implements HtmlPageFilter {
        private HtmlPageFilter filter;
        private HtmlElement img = null;
        private URL base;
        private URL requestURL;

        public Filter(HtmlPageFilter filter, URL base, URL requestURL) {
            this.filter = filter;
            this.base = base;
            this.requestURL = requestURL;
        }

        public HtmlElement getImage() {
            return this.img;
        }

        @Override
        public NodeResult filter(HtmlContent node) {
            NodeResult result = this.filter.filter(node);
            if (node instanceof HtmlElement) {
                HtmlElement elem = (HtmlElement) node;
                if ("img".equalsIgnoreCase(elem.getName())) {
                    if (this.img == null) {
                        processURL(elem, "src");
                        this.img = elem;
                    }
                    return NodeResult.skip;
                } else if ("a".equalsIgnoreCase(elem.getName())) {
                    processURL(elem, "href");
                }
            }
            return result;
        }

        @Override
        public boolean match(HtmlPage page) {
            return this.filter.match(page);
        }

        private void processURL(HtmlElement elem, String srcAttr) {
            if (elem.getAttribute(srcAttr) == null) {
                return;
            }
            HtmlAttribute attr = elem.getAttribute(srcAttr);
            if (attr == null || !attr.hasValue()) {
                return;
            }
            String val = attr.getValue();
            URL url = null;
            if (val.startsWith("http://") || val.startsWith("https://")) {
                try {
                    url = URL.parse(val);
                } catch (Throwable t) {
                    return;
                }
            } else {
                if (val.contains("#")) {
                    val = val.substring(0, val.indexOf("#"));
                }
                String query = "";
                if (val.contains("?")) {
                    query = val.substring(val.indexOf("?"));
                    val = val.substring(0, val.indexOf("?"));
                }
                Path newPath = null;
                try {
                    if (val.startsWith("/")) {
                        newPath = Path.fromString(val);
                    } else {
                        newPath = this.base.getPath().expand(val);
                    }
                } catch (Throwable t) {
                    return;
                }
                url = new URL(this.base);
                url.setPath(newPath);
                url.clearParameters();
                if (!"".equals(query.trim())) {
                    Map<String, String[]> split = URL.splitQueryString(query);
                    for (String s: split.keySet()) {
                        for (String v: split.get(s)) {
                            url.addParameter(s, v);
                        }
                    }
                }
            }
            url.setCollection(false);
            attr.setValue(url.toString());
            
            if (url.getHost().equals(this.requestURL.getHost())) {
                attr.setValue(url.getPathRepresentation());
            }
        }
    }
    
    
    protected List<String> getElementOrder(String param, DecoratorRequest request) {
        List<String> resultOrder = new ArrayList<String>();

        String[] order = null;
        try {
            order = request.getStringParameter(param).split(",");
        } catch (Exception e) {
        }

        if (order == null) {
            return getDefaultElementOrder();
        }

        // check and add
        for (int i = 0; i < order.length; i++) {
            if (order[i] != null && !getDefaultElementOrder().contains(order[i].trim())) {
                throw new DecoratorComponentException("Illigal element '" + order[i] + "' in '" + param + "'");
            }
            if (order[i] != null) {
                resultOrder.add(order[i].trim());
            }
        }
        return resultOrder;
    }

    protected URL getBaseURL(String feedURL, URL requestURL) {
        if (feedURL.contains("#")) {
            feedURL = feedURL.substring(0, feedURL.indexOf("#"));
        }
        if (feedURL.startsWith("?")) {
            URL url = new URL(requestURL.getProtocol(), 
                    requestURL.getHost(), requestURL.getPath());
            feedURL = url.toString() + feedURL;
        }
        if (feedURL.startsWith(".")) {
            URL url = new URL(requestURL.getProtocol(),
                    requestURL.getHost(), requestURL.getPath());
            feedURL = url.toString() + feedURL.substring(1);
        }
        if (!feedURL.startsWith("/")) {
            URL url = URL.parse(feedURL);
            url.clearParameters();
            url.setCollection(false);
            return url;
        }
        if (feedURL.contains("?")) {
            feedURL = feedURL.substring(0, feedURL.indexOf("?"));
        }
        boolean collection = false;
        if (feedURL.endsWith("/") && !"/".equals(feedURL)) {
            collection = true;
            feedURL = feedURL.substring(0, feedURL.length() - 1);
        }
        URL baseURL = new URL(requestURL.getProtocol(), requestURL.getHost(), 
                Path.fromString(feedURL));
        baseURL.setCollection(collection);
        return baseURL;
    }
    
    public void setImgHtmlFilter(HtmlPageFilter imgHtmlFilter) {
        this.imgHtmlFilter = imgHtmlFilter;
    }

    public HtmlPageFilter getImgHtmlFilter() {
        return imgHtmlFilter;
    }

    public void setNoImgHtmlFilter(HtmlPageFilter noImgHtmlFilter) {
        this.noImgHtmlFilter = noImgHtmlFilter;
    }

    public HtmlPageFilter getNoImgHtmlFilter() {
        return noImgHtmlFilter;
    }

    public void setDefaultElementOrder(List<String> defaultElementOrder) {
        this.defaultElementOrder = defaultElementOrder;
    }

    public List<String> getDefaultElementOrder() {
        return defaultElementOrder;
    }
}
