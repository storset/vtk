/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.web.decorating.components;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.util.cache.loaders.URLConnectionCacheLoader;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;


/**
 * A cache loader that produces {@link SyndFeed} objects.
 */
public class SyndFeedLoader extends URLConnectionCacheLoader<SyndFeed> {

    private String clientIdentifier = "Anonymous Feed Fetcher";
    private HtmlPageParser htmlParser;
    private HtmlPageFilter safeHtmlFilter;
    
    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }
    
    protected void setConnectionProperties(URLConnection connection) {
        super.setConnectionProperties(connection);
        connection.setRequestProperty("User-Agent", this.clientIdentifier);
    }

    protected SyndFeed handleConnection(URLConnection connection) throws Exception {
        InputStream stream = connection.getInputStream();
        XmlReader xmlReader = new XmlReader(stream);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(xmlReader);

        for (Object o : feed.getEntries()) {
            SyndEntry entry = (SyndEntry) o;
            SyndContent desc = entry.getDescription();
            if (desc != null) {
                String value = desc.getValue();
                if (value == null) {
                    continue;
                }

                String type = desc.getType();
                if (type != null && type.equals("xhtml")) {
                    HtmlFragment frag = this.htmlParser.parseFragment(value);
                    filterXhtml(frag);
                    desc.setValue(frag.getStringRepresentation());

                } else if (type.equals("text/html") && desc.getValue() != null) {
                    HtmlFragment frag = this.htmlParser.parseFragment(value);
                    filterHtml(frag);
                    desc.setValue(frag.getStringRepresentation());

                } else {
                    HtmlFragment frag = this.htmlParser.parseFragment(value);
                    filterText(frag);
                    desc.setValue(frag.getStringRepresentation());
                }
            }
        }
        feed.setUri(connection.getURL().toExternalForm());
        return feed;
    }
    
    private void filterXhtml(HtmlFragment fragment) {
        fragment.filter(this.safeHtmlFilter);

        final Set<HtmlContent> toplevel = new HashSet<HtmlContent>(fragment.getContent());
        fragment.filter(new HtmlPageFilter() {
            public NodeResult filter(HtmlContent node) {
                if (toplevel.contains(node)) {
                   if (node instanceof HtmlElement) {
                       return NodeResult.skip;
                   }
                   return NodeResult.exclude;
                }
                return NodeResult.keep;
            }});
    }

    private void filterHtml(HtmlFragment fragment) {
        fragment.filter(this.safeHtmlFilter);
    }

    private void filterText(HtmlFragment fragment) {
        fragment.filter(this.safeHtmlFilter);
    }
    
    @Required
    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    @Required
    public void setSafeHtmlFilter(HtmlPageFilter safeHtmlFilter) {
        this.safeHtmlFilter = safeHtmlFilter;
    }
}
