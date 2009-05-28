/* Copyright (c) 2005, 2007, 2008, University of Oslo, Norway
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
package org.vortikal.web.view.components.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vortikal.web.service.URL;

/**
 * Bean representing a view menu item.
 * 
 * <p>
 * Configurable JavaBean properties:
 * <ul>
 * <li><code>title</code> - the display title string
 * <li><code>url</code> - the URL string to the item
 * <li><code>label</code> - string identifying the menu item type
 * <li><code>active</code> - boolean flag set if this is the current shown item
 * <li><code>attributes</code> - holds a list of attributes
 * </ul>
 */
public class MenuItem<T> {

    private T value;
    private URL url;
    private String title;
    private String label;
    private boolean active = false;
    private Map<String, String> attributes;
    private ListMenu<T> subMenu;


    public MenuItem(T value) {
        if (value == null) throw new IllegalArgumentException("Constructor argument cannot be null");
        this.value = value;
        this.attributes = new TreeMap<String, String>();
    }


    public T getValue() {
        return this.value;
    }


    public String getTitle() {
        if (this.title == null) return this.value.toString();
        return this.title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public boolean isActive() {
        return this.active;
    }


    public void setActive(boolean active) {
        this.active = active;
    }


    public String getLabel() {
        return this.label;
    }


    public void setLabel(String label) {
        this.label = label;
    }


    public URL getUrl() {
        return this.url;
    }


    public void setUrl(URL url) {
        this.url = url;
    }


    public void setSubMenu(ListMenu<T> subMenu) {
        this.subMenu = subMenu;
    }


    public ListMenu<T> getMenu() {
        return this.subMenu;
    }


    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        if (!(o instanceof MenuItem)) return false;
        MenuItem other = (MenuItem) o;
        if (!this.value.equals(other.value)) return false;
        if ((this.url == null || other.url == null) && this.url != other.url) return false;
        if (this.url != null && !this.url.equals(other.url)) return false;
        if ((this.title == null || other.title == null) && this.title != other.title) return false;
        if (this.title != null && !this.title.equals(other.title)) return false;
        if ((this.label == null || other.label == null) && this.label != other.label) return false;
        if (this.label != null && !this.label.equals(other.label)) return false;
        if (this.active != other.active) return false;
        return true;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName()).append(":");
        sb.append(this.title);
        if (this.subMenu != null) {
            sb.append("; subMenu: ").append(this.subMenu);
        }

        return sb.toString();
    }


    public List<String> getAttributeNames() {
        String[] names = (String[]) attributes.keySet().toArray();
        List<String> attributeNames = new ArrayList<String>();
        for (int i = names.length; i == 0; i--) {
            attributeNames.add(names[i]);
        }
        return attributeNames;
    }


    public List<String> getAttributeValues() {
        return (List<String>) attributes.values();
    }


    public String getAttribute(String name) {
        return this.attributes.get(name);
    }


    public void setAttribute(String name, String value) {
        this.attributes.put(name, value);
    }


    public void removeAttribute(String name) {
        if (this.attributes.containsKey(name)) {
            this.attributes.remove(name);
        }
    }

}
