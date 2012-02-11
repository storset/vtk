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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.vortikal.util.text.JSON;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import net.sf.json.JSONArray;

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
                JSONObject json = JSONObject.fromObject(jsonString);
                if (json.has("contentLinks")) {
                    for (Object link: json.getJSONArray("contentLinks")) {
                        collector.add((String)link, LinkSource.CONTENT);
                    }
                    
                    evaluateContent = false;
                }
            }
        } catch (Throwable t) {
            // Some error in old property value, ignore and start fresh
            collector.clear();
        }
        
        try {
            Resource resource = ctx.getNewResource();
            for (Property p: resource) {
                if (p.getType() == PropertyType.Type.IMAGE_REF) {
                    collector.add(p.getValue().getStringValue(), LinkSource.PROPERTIES);
                } else if (p.getType() == PropertyType.Type.HTML) {
                    InputStream is = new ByteArrayInputStream(p.getStringValue().getBytes());
                    extractLinks(is, collector, LinkSource.PROPERTIES);
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
                                    extractLinks(is, collector, LinkSource.CONTENT);
                                }
                            }
                        }
                    }
                } else if ("text/html".equals(resource.getContentType())) {
                    extractLinks(ctx.getContent().getContentInputStream(), collector, LinkSource.CONTENT);
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
    
    private class LinkCollector {
        private Set<String> propertyLinks = new HashSet<String>();
        private Set<String> contentLinks = new HashSet<String>();

        public void add(String link, LinkSource source) {
            if (source == LinkSource.PROPERTIES) {
                propertyLinks.add(link);
            } else {
                contentLinks.add(link);
            }
        }
        public void clear() {
            this.propertyLinks.clear();
            this.contentLinks.clear();
        }
        public boolean isEmpty() {
            return this.propertyLinks.isEmpty() && this.contentLinks.isEmpty();
        }
        public byte[] serialize() throws Exception {
            JSONObject obj = new JSONObject();
            JSONArray arr = new JSONArray();
            arr.addAll(this.propertyLinks);
            obj.put("propertyLinks", arr);

            arr = new JSONArray();
            arr.addAll(this.contentLinks);
            obj.put("contentLinks", arr);
            
            return obj.toString().getBytes("utf-8");
        }
    }

    
    private void extractLinks(InputStream is, LinkCollector listener, LinkSource source) throws Exception {
        org.ccil.cowan.tagsoup.Parser parser
        = new org.ccil.cowan.tagsoup.Parser();
        Handler handler = new Handler(listener, source);
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

    private static class Handler implements ContentHandler {
        private LinkCollector listener;
        private LinkSource linkSource;

        public Handler(LinkCollector listener, LinkSource source) {
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
                    
                    if ("a".equals(localName) && "href".equals(attrName) 
                            || "img".equals(localName) && "src".equals(attrName)
                            || "script".equals(localName) && "src".equals(attrName)
                            || "link".equals(localName) && "href".equals(attrName)
                            || "frame".equals(localName) && "src".equals(attrName)
                            || "iframe".equals(localName) && "src".equals(attrName)) {
                        if (attrValue != null) {
                            this.listener.add(attrValue, this.linkSource);
                        }
                    }
                }                
            }
        }

        @Override
        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
        }

        @Override
        public void ignorableWhitespace(char[] chars, int start, int length)
                throws SAXException {
        }

        @Override
        public void processingInstruction(String target, String data)
                throws SAXException {
        }

        @Override
        public void setDocumentLocator(Locator locator) {
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void characters(char[] arg0, int arg1, int arg2)
                throws SAXException {
        }
        @Override
        public void endElement(String arg0, String arg1, String arg2)
                throws SAXException {
        }

    }
    
    @Required
    public void setResourceManager(StructuredResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
