/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceWrapper;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.service.URL;

public class Listing {
    
    private ResourceWrapper resource;
    private String title;
    private String name;
    private int offset;
    private boolean more;
    private List<PropertySet> files = new ArrayList<PropertySet>();
    private Map<String, URL> urls = new HashMap<String, URL>();
    private List<PropertyTypeDefinition> displayPropDefs = new ArrayList<PropertyTypeDefinition>();
    private int totalHits; /* Regardless of number of files (total its in full search) */

    public Listing(ResourceWrapper resource, String title, String name, int offset) {
        this.resource = resource;
        this.title = title;
        this.name = name;
        this.offset = offset;
    }

    public ResourceWrapper getResource() {
        return resource;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setFiles(List<PropertySet> files) {
        this.files = files;
    }

    public List<PropertySet> getFiles() {
        return files;
    }

    public int size() {
        return this.files.size();
    }

    public void setUrls(Map<String, URL> urls) {
        this.urls = urls;
    }

    public Map<String, URL> getUrls() {
        return urls;
    }

    public void setDisplayPropDefs(List<PropertyTypeDefinition> displayPropDefs) {
        this.displayPropDefs = displayPropDefs;
    }

    public List<PropertyTypeDefinition> getDisplayPropDefs() {
        return displayPropDefs;
    }

    // Need this because of structured resource namespace
    // God damn it....
    public boolean hasDisplayPropDef(String propDefName) {
        for (PropertyTypeDefinition def : this.displayPropDefs) {
            if (def.getName().equals(propDefName))
                return true;
        }
        return false;
    }

    public void setMore(boolean more) {
        this.more = more;
    }

    public boolean hasMoreResults() {
        return more;
    }

    public boolean hasContent() {
        return getFiles() != null && getFiles().size() > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(": title: " + this.title);
        sb.append("; resource: ").append(this.resource.getURI());
        sb.append("; offset: " + this.offset);
        sb.append("; size: " + this.files.size());
        sb.append("; more:").append(this.more);
        return sb.toString();
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    public int getTotalHits() {
        return totalHits;
    }
}