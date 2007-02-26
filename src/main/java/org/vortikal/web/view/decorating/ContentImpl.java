package org.vortikal.web.view.decorating;

public class ContentImpl implements Content {

    private String content;
    
    public ContentImpl(String content) {
        this.content = content;
    }

    public ContentImpl() {
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
