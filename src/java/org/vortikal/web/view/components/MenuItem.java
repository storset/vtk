package org.vortikal.web.view.components;

/** Bean representing a view menu item.
 * <p>Has the following properties:
 * <ul>
 * <li><b>title</b> - the display title string
 * <li><b>url</b> - the URL string to the item
 * <li><b>label</b> - string identifying the menu item type
 * <li><b>active</b> - boolean flag set if this is the current shown item
 *
 */
public class MenuItem {

    private String url;
    private String title;
    private String label;
    private boolean active;
    
    public boolean isActive() {
        return active;
    }
   
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
}
