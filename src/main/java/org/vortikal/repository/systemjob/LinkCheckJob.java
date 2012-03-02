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
package org.vortikal.repository.systemjob;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.text.JSONDefaultHandler;
import org.vortikal.web.display.linkcheck.LinkChecker;
import org.vortikal.web.display.linkcheck.LinkChecker.LinkCheckResult;
import org.vortikal.web.service.CanonicalUrlConstructor;
import org.vortikal.web.service.URL;

public class LinkCheckJob extends RepositoryJob {
    private PathSelector pathSelector;
    private PropertyTypeDefinition linkCheckPropDef;
    private PropertyTypeDefinition linksPropDef;
    private LinkChecker linkChecker;
    private CanonicalUrlConstructor urlConstructor;
    private static final int MAX_BROKEN_LINKS = 100;

    
    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public void executeWithRepository(final Repository repository,
            final SystemChangeContext context) throws Exception {
        if (repository.isReadOnly()) {
            return;
        }

        final String token = SecurityContext.exists() ? SecurityContext.getSecurityContext().getToken() : null;

        this.pathSelector.selectWithCallback(repository, context, new PathSelectCallback() {

            int count = 0;
            int total = -1;

            @Override
            public void beginBatch(int total) {
                this.total = total;
                this.count = 0;
                logger.info("Running job " + getId()
                        + ", " + (this.total >= 0 ? this.total : "?") + " resource(s) selected in batch.");
            }

            @Override
            public void select(Path path) throws Exception {
                ++this.count;
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invoke job " + getId() + " on " + path
                                + " [" + this.count + "/" + (this.total > 0 ? this.total : "?") + "]");
                    }
                    Resource resource = repository.retrieve(token, path, false);
                    
                    Date lastMod = resource.getLastModified();
                    Property prop = linkCheck(resource, context);
                    if (prop == null) {
                        return;
                    }
                    boolean locked = false;
                    try {
                        resource = repository.lock(token, path, context.getJobName(), Depth.ZERO, 60, null);
                        locked = true;
                        if (!lastMod.equals(resource.getLastModified())) {
                            logger.warn("Resource " + path + " was modified during link check, skipping");
                            return;
                        }
                        resource.addProperty(prop);
                        repository.store(token, resource, context);
                    } finally {
                       if (locked) {
                           repository.unlock(token, resource.getURI(), resource.getLock().getLockToken());
                       }
                    }

                } catch (ResourceNotFoundException rnfe) {
                    // Resource is no longer there after search (deleted, moved
                    // or renamed)
                    logger.warn("A resource ("
                            + path
                            + ") that was to be affected by a systemjob was no longer available: "
                            + rnfe.getMessage());
                }
            }
        });

    }

    
    private Property linkCheck(Resource resource, SystemChangeContext context) {

        Property linksProp = resource.getProperty(this.linksPropDef);
        if (linksProp == null) {
            return null;
        }
        
        Property statusProp = resource.getProperty(this.linkCheckPropDef);
        final Status status = Status.create(statusProp);
        
        ContentStream stream = linksProp.getBinaryStream();
        JSONParser parser = new JSONParser();
        
        final LinkChecker linkChecker = this.linkChecker;
        final URL base = this.urlConstructor.canonicalUrl(resource).setImmutable();
        final AtomicInteger number = new AtomicInteger(0);
        
        try {
            parser.parse(new InputStreamReader(stream.getStream()), new JSONDefaultHandler() {

                boolean url = false;

                @Override
                public void endJSON() throws ParseException, IOException {
                    status.complete = true;
                }

                @Override
                public boolean startObjectEntry(String key) throws ParseException,
                IOException {
                    this.url = "url".equals(key);
                    return true;
                }

                @Override
                public boolean endObjectEntry() throws ParseException, IOException {
                    this.url = false;
                    return true;
                }

                @Override
                public boolean primitive(Object value) throws ParseException,
                IOException {
                    if (value == null || !this.url) {
                        return true;
                    }
                    int i = number.incrementAndGet();
                    if (i < status.index) {
                        return true;
                    }
                    
                    String val = value.toString();
                    
                    if (!shouldCheck(val)) {
                        status.index++;
                        return true;
                    }
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
            status.timestamp = context.getTime();
            Property result = this.linkCheckPropDef.createProperty();
            System.out.println("__created_property: " + result);
            status.write(result);
            return result;
        } catch (Throwable t) {
            logger.warn("Error checking links for " + resource.getURI(), t);
            return null;
        }
    }

    private boolean shouldCheck(String href) {
        if (href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("ftp:")) {
            // XXX: need better heuristic
            return false;
        }
        return true;
    }

    @Required
    public void setLinksPropDef(PropertyTypeDefinition linksPropDef) {
        this.linksPropDef = linksPropDef;
    }

    @Required
    public void setPathSelector(PathSelector pathSelector) {
        this.pathSelector = pathSelector;
    }

    @Required
    public void setLinkCheckPropDef(PropertyTypeDefinition linkCheckPropDef) {
        this.linkCheckPropDef = linkCheckPropDef;
    }

    @Required
    public void setLinkChecker(LinkChecker linkChecker) {
        this.linkChecker = linkChecker;
    }
    
    @Required
    public void setCanonicalUrlConstructor(CanonicalUrlConstructor urlConstructor) {
        this.urlConstructor = urlConstructor;
    }

    private static class Status {
        private List<String> brokenLinks = new ArrayList<String>();
        private int index = 0;
        private String timestamp = null;
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
                            s.timestamp = obj.toString();
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
            obj.put("timestamp", this.timestamp);
            obj.put("index", String.valueOf(this.index));
            return obj;
        }
        
        @Override
        public String toString() {
            return toJSONObject().toJSONString();
        }
    }    
}
