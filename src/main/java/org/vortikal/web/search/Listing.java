/**
 * 
 */
package org.vortikal.web.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vortikal.edit.editor.ResourceWrapper;
import org.vortikal.repository.PropertySet;
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


    public void setMore(boolean more) {
        this.more = more;
    }


    public boolean hasMoreResults() {
        return more;
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
}