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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.web.decorating.DecoratorRequest;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

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

    @SuppressWarnings("unchecked")
    protected Map<String,String> getFilteredEntryValues(HtmlPageFilter filter, SyndFeed feed) throws Exception {
        Map<String,String> result = new LinkedHashMap<String,String>();
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            String htmlFragment = null;
            if (entry.getDescription() != null)
                htmlFragment = entry.getDescription().getValue();
            HtmlFragment fragment = this.parser.parseFragment(htmlFragment);
            fragment.filter(filter);
            result.put(entry.toString(),fragment.getStringRepresentation());
        }
        return result;
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

    protected Map<String, String> excludeEverythingButFirstTag(Map<String, String> list) {
        for (Entry<String, String> entry : list.entrySet()) {
            String value = entry.getValue();
            int l_index = -1;
            int r_index = -1;
            if (value != null) {
                l_index = value.indexOf("<");
                r_index = value.indexOf(">");
            }
            String key = entry.getKey();
            if (r_index > -1 && l_index > -1) {
                list.put(key, value.subSequence(l_index, r_index + 1).toString());
            } else {
                list.put(key, null);
            }
        }
        return list;
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
