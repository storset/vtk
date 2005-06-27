package org.vortikal.web.view.components.menu;

/**
 * Bean representing a simple menu.
 * 
 * <p>has the following properties:
 * <ul>
 * <li><b>label</b> - string identifying the menu type
 * <li><b>items</b> - array of the contained {@link MenuItem}s
 * <li><b>activeItem</b> - reference to the currently active item, if it's in this menu
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
