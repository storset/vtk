package org.vortikal.web.view.components.menu;

/**
 * Bean representing a simple menu.
 * 
 * <p>Configurable JavaBean properties:
 * <ul>
 * <li><code>label</code> - string identifying the menu type
 * <li><code>items</code> - array of the contained {@link MenuItem}s
 * <li><code>activeItem</code> - reference to the currently active item, if it's in this menu
 */
public class ListMenu {

    private String label;
    private MenuItem[] items;
    private MenuItem activeItem;

    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public MenuItem getActiveItem() {
        return activeItem;
    }

    public void setActiveItem(MenuItem activeItem) {
        this.activeItem = activeItem;
    }
    
    public MenuItem[] getItems() {
        return items;
    }
    
    public void setItems(MenuItem[] items) {
        this.items = items;
    }
        
}
