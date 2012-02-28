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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContentHandler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.LatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.display.linkcheck.LinkChecker;
import org.vortikal.web.display.linkcheck.LinkChecker.LinkCheckResult;
import org.vortikal.web.service.URL;

public class LinkCheckEvaluator implements LatePropertyEvaluator {

    private PropertyTypeDefinition linksPropDef;
    private LinkChecker linkChecker;
    //private Service urlConstructor;
    private URL baseURL;
    
    private static final int MAX_BROKEN_LINKS = 100;

    

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx)
            throws PropertyEvaluationException {

        if (ctx.getEvaluationType() != PropertyEvaluationContext.Type.SystemPropertiesChange) {
            return false;
        }
        
        Property linksProp = ctx.getNewResource().getPropertyByPrefix(null, "links");
        if (linksProp == null) {
            return false;
        }
        
        Property statusProp = ctx.getOriginalResource().getProperty(property.getDefinition());
        final Status status = Status.create(statusProp);
        
        ContentStream stream = linksProp.getBinaryStream();
        JSONParser parser = new JSONParser();
        
        final LinkChecker linkChecker = this.linkChecker;
        final URL base = this.baseURL;
        final AtomicInteger number = new AtomicInteger(0);
        
        try {
            parser.parse(new InputStreamReader(stream.getStream()), new ContentHandler() {

                boolean url = false;

                @Override
                public void startJSON() throws ParseException, IOException {
                }
                
                @Override
                public void endJSON() throws ParseException, IOException {
                    status.complete = true;
                }

                @Override
                public boolean startObject() throws ParseException, IOException {
                    return true;
                }

                @Override
                public boolean endObject() throws ParseException, IOException {
                    return true;
                }

                @Override
                public boolean startObjectEntry(String key) throws ParseException,
                IOException {
                    if ("url".equals(key)) {
                        url = true;
                        return true;
                    }
                    url = false;
                    return true;
                }

                @Override
                public boolean endObjectEntry() throws ParseException, IOException {
                    url = false;
                    return true;
                }

                @Override
                public boolean startArray() throws ParseException, IOException {
                    return true;
                }

                @Override
                public boolean endArray() throws ParseException, IOException {
                    return true;
                }

                @Override
                public boolean primitive(Object value) throws ParseException,
                IOException {
                    if (value == null || !url) {
                        return true;
                    }
                    int i = number.incrementAndGet();
                    if (i < status.index) {
                        return true;
                    }
                    
                    String val = value.toString();
                    LinkCheckResult result = linkChecker.validate(val, base);
                    status.index++;
                    if (!"OK".equals(result.getStatus())) {
                        status.brokenLinks.add(val);
                    }
                    if (status.brokenLinks.size() == MAX_BROKEN_LINKS) {
                        return false;
                    }
                    return true;
                }

            });
            status.timestamp = ctx.getTime();
            status.write(property);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Required
    public void setLinksPropDef(PropertyTypeDefinition linksPropDef) {
        this.linksPropDef = linksPropDef;
    }

    @Required
    public void setLinkChecker(LinkChecker linkChecker) {
        this.linkChecker = linkChecker;
    }
    
    @Required
    public void setBaseURL(String baseURL) {
        this.baseURL = URL.parse(baseURL).setImmutable();
    }

//    @Required
//    public void setUrlConstructor(Service urlConstructor) {
//        this.urlConstructor = urlConstructor;
//    }
//

    private static class Status {
        private List<String> brokenLinks = new ArrayList<String>();
        private int index = 0;
        private Date timestamp = new Date();
        private boolean complete = false;
        
        private Status() {}
        
        @SuppressWarnings("unchecked")
        private static Status create(Property statusProp) {
            Status s = new Status();
            if (statusProp != null) {
                try {
                    Object o = JSONValue.parse(new InputStreamReader(statusProp.getBinaryStream().getStream()));
                    JSONObject status = (JSONObject) o;
                    if (status != null) {
                        Object obj = status.get("status");
                        if (obj != null) {
                            s.complete = "COMPLETE".equals(obj.toString());
                        }
                        obj = status.get("brokenLinks");
                        if (obj != null) {
                            List<String> list = (List<String>) obj;
                            for (String str: list) {
                                s.brokenLinks.add(str);
                            }
                        }
                        obj = status.get("index");
                        if (obj != null) {
                            s.index = Integer.parseInt(obj.toString());
                        }
                        obj = status.get("timestamp");
                        if (obj != null) {
                            long millis = Long.parseLong(obj.toString());
                            s.timestamp = new Date(millis);
                        }
                    }
                } catch (Throwable t) { }
            }
            return s;
        }
        
        public void write(Property statusProp) throws Exception {
            JSONObject obj = toJSONObject();
            statusProp.setBinaryValue(obj.toJSONString().getBytes("utf-8"), "application/json");
        }
        
        @SuppressWarnings("unchecked")
        private JSONObject toJSONObject() {
            JSONObject obj = new JSONObject();
            if (this.brokenLinks != null) {
                obj.put("brokenLinks", this.brokenLinks);
            }
            obj.put("status", this.complete ? "COMPLETE" : "INCOMPLETE");
            obj.put("timestamp", String.valueOf(this.timestamp.getTime()));
            obj.put("index", String.valueOf(this.index));
            return obj;
        }
        
        @Override
        public String toString() {
            return toJSONObject().toJSONString();
        }
    }
}
