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
package vtk.web.decorating.components;

import java.util.ArrayList;
import java.util.List;

import vtk.repository.AuthorizationException;
import vtk.repository.Repository;
import vtk.repository.Resource;
import vtk.repository.ResourceNotFoundException;
import vtk.security.AuthenticationException;
import vtk.text.html.HtmlContent;
import vtk.text.html.HtmlElement;
import vtk.text.html.HtmlFragment;
import vtk.text.html.HtmlPage;
import vtk.text.html.HtmlPageFilter;
import vtk.text.html.HtmlPageParser;
import vtk.text.html.HtmlUtil;
import vtk.web.RequestContext;
import vtk.web.decorating.DecoratorRequest;
import vtk.web.service.URL;

import com.sun.syndication.feed.synd.SyndEntry;

public abstract class AbstractFeedComponent extends ViewRenderingDecoratorComponent {

    protected HtmlUtil htmlUtil;
    protected HtmlPageFilter safeHtmlFilter;

    protected static final String PARAMETER_ITEM_PICTURE = "item-picture";
    protected static final String PARAMETER_ITEM_PICTURE_DESC = "Must be set to 'true' to show item picture";

    protected static final String PARAMETER_IF_EMPTY_MESSAGE = "if-empty-message";
    protected static final String PARAMETER_IF_EMPTY_MESSAGE_DESC = "Message to be displayd if feed is empty";

    protected static final String PARAMETER_FEED_ELEMENT_ORDER = "element-order";
    protected static final String PARAMETER_FEED_ELEMENT_ORDER_DESC = "The order that the elementes are listed";

    protected static final String PARAMETER_ALLOW_MARKUP = "allow-markup";
    protected static final String PARAMETER_ALLOW_MARKUP_DESC = "Set to 'true' to include span elements and class attributes";

    
    private HtmlPageParser parser = new HtmlPageParser();
    private List<String> defaultElementOrder;

    /**
     * Retrieves the resource corresponding to a local feed for authorization
     * purposes
     */
    protected Resource retrieveLocalResource(URL feedURL) throws ResourceNotFoundException,
    AuthorizationException, AuthenticationException, Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.isViewUnauthenticated() ? null : requestContext.getSecurityToken(); // VTK-2460
        return repository.retrieve(token, feedURL.getPath(), true);
    }

    boolean parameterHasValue(String param, String includeParamValue, DecoratorRequest request) {
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

    protected HtmlElement removeImage(HtmlFragment fragment) {
        RemoveImageFilter filter = new RemoveImageFilter();
        fragment.filter(filter);
        return filter.getImageElement();
    }

    private static class RemoveImageFilter implements HtmlPageFilter {

        private HtmlElement image = null;

        @Override
        public boolean match(HtmlPage page) {
            return true;
        }

        @Override
        public NodeResult filter(HtmlContent node) {
            if (node instanceof HtmlElement) {

                HtmlElement elem = (HtmlElement) node;
                if (elem.getName().equals("img")) {
                    if (image == null)
                        image = elem;

                    return NodeResult.exclude;

                }

            }
            return NodeResult.keep;
        }

        public HtmlElement getImageElement() {
            return image;
        }
    }

    protected HtmlFragment getDescription(SyndEntry entry, URL baseURL, URL requestURL, boolean filter) throws Exception {
        if (entry.getDescription() == null) {
            return null;
        }

        String value = null;
        HtmlFragment html;
        
        if (filter) {
            html = filterEntry(entry, safeHtmlFilter);
            value = html.getStringRepresentation();
        } else {
            value = entry.getDescription().getValue();
        }
        
        if (value == null) {
            return null;
        }
        
        return htmlUtil.linkResolveFilter(value, baseURL, requestURL, false);
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

    public void setDefaultElementOrder(List<String> defaultElementOrder) {
        this.defaultElementOrder = defaultElementOrder;
    }

    public List<String> getDefaultElementOrder() {
        return defaultElementOrder;
    }

    public void setHtmlUtil(HtmlUtil htmlUtil) {
        this.htmlUtil = htmlUtil;
    }

    public HtmlUtil getHtmlUtil() {
        return htmlUtil;
    }

    public void setSafeHtmlFilter(HtmlPageFilter safeHtmlFilter) {
        this.safeHtmlFilter = safeHtmlFilter;
    }

}
