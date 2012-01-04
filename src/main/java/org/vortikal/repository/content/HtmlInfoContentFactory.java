/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.repository.content;

import java.util.Stack;

import org.vortikal.util.text.TextUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class HtmlInfoContentFactory implements ContentFactory {

    @Override
    public Class<?>[] getRepresentationClasses() {
        return new Class[] { HtmlInfo.class };
    }

    @Override
    public Object getContentRepresentation(Class<?> clazz,
            InputStreamWrapper content) throws Exception {
        HtmlInfo map = new HtmlInfo();
        org.ccil.cowan.tagsoup.Parser parser
            = new org.ccil.cowan.tagsoup.Parser();
        Handler handler = new Handler(map);
        parser.setContentHandler(handler);
        parser.setProperty(org.ccil.cowan.tagsoup.Parser.lexicalHandlerProperty, handler);
        InputSource input = new InputSource(content);
        
        try {
            parser.parse(input);
        } catch (StopException t) { 
        } finally {
            content.close();
        }
        return map;
    }
    
    @SuppressWarnings("serial")
    private static class StopException extends RuntimeException { }

    private static class Handler implements ContentHandler, LexicalHandler {
        private HtmlInfo htmlInfo;
        private Stack<String> stack = new Stack<String>();

        public Handler(HtmlInfo map) {
            this.htmlInfo = map;
        }
        
        @Override
        public void startElement(String namespaceUri, String localName, String qName,
                Attributes attrs) throws SAXException {
            
            this.stack.push(localName);
            
            if ("meta".equals(localName)) {
                boolean httpEquiv = false;
                String charset = null;
                String html5charset = null;
                for (int i = 0; i < attrs.getLength(); i++) {
                    String attrName = attrs.getQName(i);
                    String attrValue = attrs.getValue(i);
                    
                    if ("charset".equals(attrName)) {
                        // HTML 5 <meta charset="...">
                        html5charset = attrValue;
                        break;
                        
                    } else if ("http-equiv".equals(attrName)) {
                        if (attrValue != null && "content-type".equals(attrValue.toLowerCase())) {
                            httpEquiv = true;
                        }
                        
                    } else if ("content".equals(attrName)) {
                        charset = TextUtils.extractField(attrValue, "charset", ";");
                    }
                }
                if (html5charset != null) {
                  this.htmlInfo.setEncoding(html5charset.toLowerCase());
                  
                } else if (httpEquiv && charset != null) {
                    this.htmlInfo.setEncoding(charset.toLowerCase());
                }
            }
        }

        @Override
        public void endElement(String namespaceUri, String localName, String qName)
                throws SAXException {
            if (!this.stack.isEmpty()) {
                this.stack.pop();
            }
        }

        @Override
        public void characters(char[] chars, int start, int length)
                throws SAXException {
            if (!this.stack.isEmpty()) {
                String top = this.stack.peek();
                if ("title".equals(top)) {
                    this.htmlInfo.setTitle(new String(chars, start, length));
                    return;
                }
                for (String elem: this.stack) {
                    if ("body".equals(elem.toLowerCase())) {
                        String s = new String(chars, start, length);
                        if (!"".equals(s.trim())) {
                            this.htmlInfo.setBody(true);
                            throw new StopException();
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
        public void startDTD(String name, String publicId, String systemId)
                throws SAXException {
            if (name != null && publicId != null && systemId != null) {
                htmlInfo.setDocType(name + " PUBLIC \"" + publicId + "\" \"" + systemId + "\"");

            } else if (name != null && publicId != null) {
                htmlInfo.setDocType(name + " PUBLIC \"" + publicId + "\"");
                
            } else if (name != null) {
                htmlInfo.setDocType(name);
            }
        }

        @Override
        public void endDTD() throws SAXException {
        }

        @Override
        public void comment(char[] chars, int start, int length)
                throws SAXException {
        }

        @Override
        public void startCDATA() throws SAXException {
        }

        @Override
        public void endCDATA() throws SAXException {
        }

        @Override
        public void startEntity(String name) throws SAXException {
        }
        
        @Override
        public void endEntity(String name) throws SAXException {
        }

    }
}
