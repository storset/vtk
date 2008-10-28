package org.vortikal.integration.webtests;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseWebTest extends AbstractWebTest {
    
    private List<String> defaultFiles;
    private List<String> events;
    private List<String> articles;
    
    protected void setUp() throws Exception {
        super.setUp();
        // Files we expect to find on a standard collectionview
        defaultFiles = new ArrayList<String>();
        defaultFiles.add("standard");
        // Files we expect to find for eventlistings
        events = new ArrayList<String>();
        events.add("previousevent");
        events.add("upcomingevent");
        // FIles we expect to find for articlelistings
        articles = new ArrayList<String>();
        articles.add("article");
    }
    
    protected List<String> getDefaultFiles() {
        return this.defaultFiles;
    }
    
    protected List<String> getEvents() {
        return this.events;
    }
    
    protected List<String> getArticles() {
        return this.articles;
    }
    
    protected String getBaseUrl() throws Exception {
        return getProperty(PROP_VIEW_URL);
    }

    @Override
    protected boolean requiresAuthentication() {
        return false;
    }

}
