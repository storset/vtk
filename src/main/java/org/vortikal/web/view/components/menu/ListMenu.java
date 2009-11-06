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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.vortikal.web.service.URL;

/**
 * Bean representing a simple menu.
 * 
 * <p>Configurable JavaBean properties:
 * <ul>
 * <li><code>title</code> - the menu title
 * <li><code>label</code> - identifies the menu type
 * <li><code>items</code> - array of the contained {@link MenuItem}s
 * <li><code>activeItem</code> - reference to the currently active item, if it's in this menu
 * </ul>
 */
public class ListMenu<T> {

    private String title;
    private String label;
    private List<MenuItem<T>> items = new ArrayList<MenuItem<T>>();
    private MenuItem<T> activeItem;
    private Comparator<MenuItem<T>> comparator;
    private URL moreUrl;

    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getLabel() {
        return this.label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public MenuItem<T> getActiveItem() {
        return this.activeItem;
    }

    public void setActiveItem(MenuItem<T> activeItem) {
        this.activeItem = activeItem;
    }
    
    public void addItem(MenuItem<T> item) {
        this.items.add(item);
    }
    
    public void addAllItems(List<MenuItem<T>> items) {
        this.items.addAll(items);
    }

    public List<MenuItem<T>> getItems() {
        return Collections.unmodifiableList(this.items);
    }
    
    public void setComparator(Comparator<MenuItem<T>> comparator) {
        this.comparator = comparator;
    }

    public List<MenuItem<T>> getItemsSorted() {
        List<MenuItem<T>> sortedList = new ArrayList<MenuItem<T>>(this.items);
        if (this.comparator == null) {
            throw new IllegalStateException("No comparator has been specified on this list menu");
        }
        Collections.sort(sortedList, this.comparator);
        return Collections.unmodifiableList(sortedList);
    }
    

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName()).append(": [");
        sb.append("title=").append(this.title);
        sb.append(", label=").append(this.label);
        sb.append(", activeItem=").append(this.activeItem);
        sb.append(", items=").append(this.items);
        sb.append(", moreurl=").append(this.moreUrl);
        sb.append("]");
        return sb.toString();
    }

    public void setMoreUrl(URL moreUrl) {
        this.moreUrl = moreUrl;
    }

    public URL getMoreUrl() {
        return moreUrl;
    }
}
