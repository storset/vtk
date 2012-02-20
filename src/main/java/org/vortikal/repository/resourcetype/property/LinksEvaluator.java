/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype.property;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.PropertyEvaluationContext.Type;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.LatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.resourcemanagement.PropertyDescription;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceManager;
import org.vortikal.util.io.StreamUtil;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.HandlerBase;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LinksEvaluator implements LatePropertyEvaluator {
    
    private StructuredResourceManager resourceManager;
    
    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {

        final LinkCollector collector = new LinkCollector();
        boolean evaluateContent = true;
        try {
            if (property.isValueInitialized()
                    && ctx.getEvaluationType() != Type.ContentChange && ctx.getEvaluationType() != Type.Create) {
                // Preserve existing content links, since this is not content change.
                InputStream stream = property.getBinaryStream().getStream();
                String jsonString = StreamUtil.streamToString(stream, "utf-8");
                JSONArray arr = JSONArray.fromObject(jsonString);

                for (Object o: arr) {
                    if (! (o instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject obj = (JSONObject) o;
                    String url = obj.getString("url");
                    String type = obj.getString("type");
                    LinkSource source = LinkSource.valueOf(obj.getString("source"));
                    if (source == LinkSource.CONTENT) {
                        Link link = new Link(url, LinkType.valueOf(type), source);
                        collector.add(link);
                    }
                }
                evaluateContent = false;
            }
        } catch (Throwable t) {
            // Some error in old property value, ignore and start fresh
            collector.clear();
        }
        
        try {
            Resource resource = ctx.getNewResource();
            for (Property p: resource) {
                if (p.getType() == PropertyType.Type.IMAGE_REF) {
                    Link link = new Link(p.getStringValue(), LinkType.PROPERTY, LinkSource.PROPERTIES);
                    collector.add(link);
                } else if (p.getType() == PropertyType.Type.HTML) {
                    InputStream is = new ByteArrayInputStream(p.getStringValue().getBytes());
                    extractLinks(is, new HtmlHandler(collector, LinkSource.PROPERTIES));
                }
            }

            if (evaluateContent && ctx.getContent() != null) {
                if ("application/json".equals(resource.getContentType())) {
                    StructuredResourceDescription desc = this.resourceManager.get(resource.getResourceType());
                    if (desc != null) {

                        StructuredResource res = desc.buildResource(ctx.getContent().getContentInputStream());

                        for (PropertyDescription pdesc : desc.getAllPropertyDescriptions()) {
                            if (pdesc.isNoExtract()) {
                                Object p = res.getProperty(pdesc.getName());
                                if (p != null) {
                                    InputStream is = new ByteArrayInputStream(p.toString().getBytes());
                                    extractLinks(is, new HtmlHandler(collector, LinkSource.CONTENT));
                                }
                            }
                        }
                    }
                } else if ("text/html".equals(resource.getContentType())) {
                    extractLinks(ctx.getContent().getContentInputStream(), new HtmlHandler(collector, LinkSource.CONTENT));
                } else if ("text/xml".equals(resource.getContentType())) {
                    extractLinks(ctx.getContent().getContentInputStream(), new XmlHandler(collector, LinkSource.CONTENT));
                }
            }

            if (collector.isEmpty()) {
                return false;
            }
            property.setBinaryValue(collector.serialize(), "application/json");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    private enum LinkSource {
        PROPERTIES,
        CONTENT
    }
    
    private enum LinkType {
        ANCHOR,
//        CSS,
        LINK,
        IMG,
        SCRIPT,
        FRAME,
        IFRAME,
        PROPERTY,
        OBJ
    }
    
    private static class Link {

        private String url;
        private LinkType type;
        private LinkSource source;
        public Link(String url, LinkType type, LinkSource source) {
            this.url = url;
            this.type = type;
            this.source = source;
        }
        public String getURL() {
            return this.url;
        }
        public LinkType getType() {
            return this.type;
        }
        public LinkSource getSource() {
            return this.source;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Link other = (Link) obj;
            if ((this.url == null) ? (other.url != null) : !this.url.equals(other.url)) {
                return false;
            }
            if (this.type != other.type) {
                return false;
            }
            if (this.source != other.source) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 37 * hash + (this.url != null ? this.url.hashCode() : 0);
            hash = 37 * hash + (this.type != null ? this.type.hashCode() : 0);
            hash = 37 * hash + (this.source != null ? this.source.hashCode() : 0);
            return hash;
        }
    }
    
    private static class LinkCollector {
        private Set<Link> links = new HashSet<Link>();

        public void add(Link link) {
            this.links.add(link);
        }
        public void clear() {
            this.links.clear();
        }
        public boolean isEmpty() {
            return this.links.isEmpty();
        }
        public byte[] serialize() throws Exception {
            JSONArray arr = new JSONArray();
            for (Link l: this.links) {
                JSONObject entry = new JSONObject();
                entry.put("url", l.getURL());
                entry.put("type", l.getType());
                entry.put("source", l.getSource());
                arr.add(entry);
            }
            return arr.toString().getBytes("utf-8");
        }
    }

    // TODO:
    // Handlers/link parsing strategies should be pluggable, keyed on content type.
        
    private void extractLinks(InputStream is, ContentHandler handler) throws Exception {

        org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
        parser.setContentHandler(handler);

        InputSource input = new InputSource(is);

        try {
            parser.parse(input);
        } catch (StopException t) { 
        } finally {
            //content.close();
        }
    }
    
    @SuppressWarnings("serial")
    private static class StopException extends RuntimeException { }

    // TODO perhaps switch to pull-parser for XML content, instead of classic SAX.
    private static class XmlHandler extends DefaultHandler {

        private LinkCollector collector;
        private LinkSource source;
        private StringBuilder buffer = new StringBuilder();
        private boolean linkData = false;
        
        XmlHandler(LinkCollector collector, LinkSource source) {
            this.collector = collector;
            this.source = source;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (localName == null) {
                return;
            }

            // TODO figure out all elements that can contain links in our XML doc types ..
            // TODO do we have dynamically created XSL-generated links ? If so, this won't help us much..
            if ("webadresse".equals(localName)) {
                linkData = true;
                buffer.delete(0, buffer.length());
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (linkData) {
                String href = buffer.toString().trim();
                if (!href.isEmpty()) {
                    collector.add(new Link(href, LinkType.ANCHOR, source));
                }
                linkData = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (linkData) {
                buffer.append(ch, start, length);
            }
        }
    }
    
    private static class HtmlHandler extends DefaultHandler {
        private LinkCollector listener;
        private LinkSource linkSource;

        public HtmlHandler(LinkCollector listener, LinkSource source) {
            this.listener = listener;
            this.linkSource = source;
        }
        
        private static final Set<String> ELEMS = new HashSet<String>(Arrays.asList(new String[]{
                "a", "img", "script", "link", "frame", "iframe"
        }));
        
        @Override
        public void startElement(String namespaceUri, String localName, String qName,
                Attributes attrs) throws SAXException {
            if (localName == null) {
                return;
            }
            localName = localName.toLowerCase();
            if (ELEMS.contains(localName)) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attrName = attrs.getQName(i);
                    String attrValue = attrs.getValue(i);
                    
                    LinkType type = null;
                    
                    if ("a".equals(localName) && "href".equals(attrName)) {
                        type = LinkType.ANCHOR;
                        
                    } else if ("img".equals(localName) && "src".equals(attrName)) {
                        type = LinkType.IMG;
                        
                    } else if ("script".equals(localName) && "src".equals(attrName)) {
                        type = LinkType.SCRIPT;
                        
                    } else if ("link".equals(localName) && "href".equals(attrName)) {
                        type = LinkType.LINK;
                        
                    } else if ("frame".equals(localName) && "src".equals(attrName)) {
                        type = LinkType.FRAME;
                        
                    } else if ("iframe".equals(localName) && "src".equals(attrName)) {
                        type = LinkType.IFRAME;
                    }
                    
                    if (type != null && attrValue != null) {
                        this.listener.add(new Link(attrValue, type, this.linkSource));
                        //this.listener.add(attrValue, this.linkSource);
                    }
                }                
            }
        }
    }
    
    @Required
    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
