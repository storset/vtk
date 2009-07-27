package org.vortikal.text.html;

public abstract class AbstractHtmlPageFilter implements HtmlPageFilter {

    public abstract NodeResult filter(HtmlContent node);

    protected HtmlElement createElement(String name, boolean xhtml, boolean emptyTag) {
        return new HtmlElementImpl(name, xhtml, emptyTag);
    }
    
    protected HtmlAttribute createAttribute(String name, String value) {
        return new HtmlAttributeImpl(name, value, false);
    }
}
