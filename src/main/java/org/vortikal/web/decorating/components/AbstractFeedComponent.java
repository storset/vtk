package org.vortikal.web.decorating.components;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.text.html.HtmlPageParserImpl;
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
    protected List<String> getFilteredEntryValues(HtmlPageFilter filter, SyndFeed feed) throws Exception {
        List<String> result = new ArrayList<String>();
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            String htmlFragment = entry.getDescription().getValue();
            HtmlPageParser parser = new HtmlPageParserImpl();
            HtmlFragment fragment = parser.parseFragment(htmlFragment);
            fragment.filter(filter);
            result.add(fragment.getStringRepresentation());
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

    protected List<String> excludeEverythingButFirstTag(List<String> list) {
        List<String> result = new ArrayList<String>();
        for (String s : list) {
            int index = -1;
            if (s != null) {
                index = s.indexOf(">");
            }
            if (index > -1) {
                result.add(s.subSequence(0, index + 1).toString());
            } else {
                result.add(s);
            }
        }
        return result;
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
