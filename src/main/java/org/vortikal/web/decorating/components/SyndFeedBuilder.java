/* Copyright (c) 2009, University of Oslo, Norway
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
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.text.html.HtmlAttribute;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.text.html.HtmlElement;
import org.vortikal.text.html.HtmlFragment;
import org.vortikal.text.html.HtmlPage;
import org.vortikal.text.html.HtmlPageFilter;
import org.vortikal.text.html.HtmlPageParser;
import org.vortikal.web.service.URL;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class SyndFeedBuilder {

    private HtmlPageParser htmlParser;
    private HtmlPageFilter safeHtmlFilter;

    @Required
    public void setHtmlParser(HtmlPageParser htmlParser) {
        this.htmlParser = htmlParser;
    }

    @Required
    public void setSafeHtmlFilter(HtmlPageFilter safeHtmlFilter) {
        this.safeHtmlFilter = safeHtmlFilter;
    }

    public SyndFeed build(InputStream is) throws Exception {
        XmlReader xmlReader = new XmlReader(is);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(xmlReader);
        clean(feed);
        return feed;
    }

    private void clean(SyndFeed feed) throws Exception {

        URL base = null;
        try {
            base = URL.parse(feed.getLink());
        } catch (Exception e) {
        }

        // fall back to uri
        try {
            base = URL.parse(feed.getUri());
        } catch (Exception e) {
        }

        for (Object o : feed.getEntries()) {
            SyndEntry entry = (SyndEntry) o;
            if (base != null) {
                String link = base.relativeURL(entry.getLink()).toString();
                entry.setLink(link);
            }
            SyndContent desc = entry.getDescription();
            if (desc != null) {
                String value = desc.getValue();
                if (value == null) {
                    continue;
                }

                String type = desc.getType();
                if (type != null) {
                    if (type.equals("xhtml")) {
                        HtmlFragment frag = this.htmlParser.parseFragment(value);
                        filterXhtml(frag, base);
                        desc.setValue(frag.getStringRepresentation());

                    } else if (type.equals("text/html") && desc.getValue() != null) {
                        HtmlFragment frag = this.htmlParser.parseFragment(value);
                        filterHtml(frag, base);
                        desc.setValue(frag.getStringRepresentation());
                    }
                } else {
                    HtmlFragment frag = this.htmlParser.parseFragment(value);
                    filterText(frag, base);
                    desc.setValue(frag.getStringRepresentation());
                }
            }
        }
    }

    private class ImageFilter implements HtmlPageFilter {
        private URL base;

        public ImageFilter(URL base) {
            this.base = base;
        }

        @Override
        public boolean match(HtmlPage page) {
            return true;
        }

        @Override
        public NodeResult filter(HtmlContent node) {
            if (node instanceof HtmlElement) {
                HtmlElement elem = (HtmlElement) node;
                if (elem.getName().equalsIgnoreCase("img")) {
                    HtmlAttribute src = elem.getAttribute("src");
                    if (src != null) {
                        String link = src.getValue();
                        if (link != null) {
                            src.setValue(base.relativeURL(link).toString());
                        }
                    }
                }
            }
            return NodeResult.keep;
        }
    }

    private void filterXhtml(HtmlFragment fragment, URL base) {
        fragment.filter(this.safeHtmlFilter);
        fragment.filter(new ImageFilter(base));

        final Set<HtmlContent> toplevel = new HashSet<HtmlContent>(fragment.getContent());
        fragment.filter(new HtmlPageFilter() {
            public boolean match(HtmlPage page) {
                return true;
            }

            public NodeResult filter(HtmlContent node) {
                if (toplevel.contains(node)) {
                    if (node instanceof HtmlElement) {
                        return NodeResult.skip;
                    }
                    return NodeResult.exclude;
                }
                return NodeResult.keep;
            }
        });
    }

    private void filterHtml(HtmlFragment fragment, URL base) {
        fragment.filter(this.safeHtmlFilter);
        fragment.filter(new ImageFilter(base));
    }

    private void filterText(HtmlFragment fragment, URL base) {
        fragment.filter(this.safeHtmlFilter);
        fragment.filter(new ImageFilter(base));
    }

}
