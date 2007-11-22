package org.vortikal.text.html;

import java.util.List;

public interface HtmlFragment {
 
    public List<HtmlContent> getContent();
    
    public void filter(HtmlPageFilter filter);

    public String getStringRepresentation();
}
