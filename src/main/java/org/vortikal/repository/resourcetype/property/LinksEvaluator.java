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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
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
import org.vortikal.text.html.TagsoupParserFactory;
import org.vortikal.util.io.StreamUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

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
                    extractLinksHtml(is, collector, LinkSource.PROPERTIES);
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
                                if (p == null) {
                                    continue;
                                }
                                if ("json".equals(pdesc.getType())) {
                                    unwrapJSON(p, collector);
                                } else {
                                    InputStream is = new ByteArrayInputStream(p.toString().getBytes());
                                    extractLinksHtml(is, collector, LinkSource.CONTENT);
                                }
                            }
                        }
                    }
                } else if ("text/html".equals(resource.getContentType())) {
                    extractLinksHtml(ctx.getContent().getContentInputStream(), collector, LinkSource.CONTENT);
                } else if ("text/xml".equals(resource.getContentType())) {
                    Document doc = (Document)ctx.getContent().getContentRepresentation(Document.class);
                    extractLinksXml(doc, collector, LinkSource.CONTENT);
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

    
    // TODO Handlers/link parsing strategies should be pluggable, keyed on content type.
        
    private void extractLinksHtml(InputStream is,
                                  LinkCollector collector,
                                  LinkSource source)
        throws Exception {
        
        org.ccil.cowan.tagsoup.Parser parser = TagsoupParserFactory.newParser(true);
        parser.setContentHandler(new HtmlHandler(collector, source));

        InputSource input = new InputSource(is);

        try {
            parser.parse(input);
        } catch (StopException t) { 
        } finally {
            is.close();
        }
    }
    
    private void extractLinksXml(Document doc,
                                 LinkCollector collector,
                                 LinkSource source) throws Exception {
        
        Iterator nodeIterator = doc.getDescendants();
        while (nodeIterator.hasNext()) {
            Object next = nodeIterator.next();
            if (! (next instanceof Element)) {
                continue;
            }

            Element e = (Element) next;
            String href = null;
            
            if ("webadresse".equals(e.getName())
                    || "url".equals(e.getName())) {
                href = e.getTextTrim();
            } else {
                Element p = e.getParentElement();
                if (p != null) {
                    if ("pensumpunkt".equals(p.getName()) || "bilde-referanse".equals(p.getName())) {
                        if ("src".equals(e.getName())
                            || "lenkeadresse".equals(e.getName())
                            || "bibsys".equals(e.getName())
                            || "fulltekst".equals(e.getName())) {
                            href = e.getTextTrim();
                        }
                    }
                }
            }
            
            if (href != null && !href.isEmpty()) {
                collector.add(new Link(href, LinkType.ANCHOR, source));
            }
        }
    }
    
    @SuppressWarnings("serial")
    private static class StopException extends RuntimeException { }

    private static class HtmlHandler extends DefaultHandler {
        private final LinkCollector listener;
        private final LinkSource linkSource;

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
                    }
                }                
            }
        }
    }

    private void unwrapJSON(Object object, LinkCollector collector) throws Exception {
        if (object instanceof List) {
            List<?> list = (List<?>) object;
            for (Object o: list) {
                unwrapJSON(o, collector);
            }
            
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Object k: map.keySet()) {
                Object o = map.get(k);
                unwrapJSON(o, collector);
            }
        } else if (!(object instanceof JSONNull)) {
            InputStream is = new ByteArrayInputStream(object.toString().getBytes());
            extractLinksHtml(is, collector, LinkSource.CONTENT);
        }
    }
    
    
    @Required
    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
