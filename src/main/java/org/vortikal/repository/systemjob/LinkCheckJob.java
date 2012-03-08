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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Lock;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
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
    private List<String> blackListConfig;
    private List<Pattern> blackList;
    private int updateBatch = 0;
    private boolean useRepositoryLocks = false;
    
    private CanonicalUrlConstructor urlConstructor;
    
    // TODO these should probably be configurable
    private static final int MAX_BROKEN_LINKS = 100;   // max number of broken links we bother storing
    private static final int MAX_CHECK_LINKS = 100;    // max number of links to check per resource per round
    private static final int MIN_RECHECK_SECONDS = 3600;
    
    private static final Log logger = LogFactory.getLog(LinkCheckJob.class);

    @Override
    public void executeWithRepository(final Repository repository,
                                      final SystemChangeContext context) throws Exception {
        if (repository.isReadOnly()) {
            return;
        }

        final String token = SecurityContext.exists() ? SecurityContext.getSecurityContext().getToken() : null;
        final UpdateBatch batch = new UpdateBatch(repository, token, context, this.updateBatch, this.useRepositoryLocks);
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
                    Property prop = linkCheck(resource, context);
                    if (prop != null) {
                        resource.addProperty(prop);
                    } else {
                        // Delete any old stale value
                        resource.removeProperty(linkCheckPropDef);
                    }
                    batch.add(resource);

                } catch (ResourceNotFoundException rnfe) {
                    // Resource is no longer there after search (deleted, moved
                    // or renamed)
                    // Maybe just log at debug-level here
                    logger.warn("A resource ("
                            + path
                            + ") that was to be affected by a systemjob was no longer available: "
                            + rnfe.getMessage());
                }
            }
        });
        batch.flush();
    }

    
    private Property linkCheck(Resource resource, SystemChangeContext context) throws InterruptedException {

        Property linksProp = resource.getProperty(this.linksPropDef);
        if (linksProp == null) {
            return null;
        }
        
        Property linkCheckProp = resource.getProperty(this.linkCheckPropDef);
        
        final LinkCheckState state = LinkCheckState.create(linkCheckProp);
        if (shouldResetState(state, resource, context)) {
            logger.debug("Reset link check state for " + resource.getURI());
            state.brokenLinks.clear();
            state.complete = false;
            state.index = 0;
        }

        if (state.complete) {
            logger.debug("Link check already complete and up to date for " + resource.getURI());
            state.write(linkCheckProp);
            return linkCheckProp;
        }

        logger.debug("Running with link check state: " + state + " for " + resource.getURI());
        
        ContentStream linksStream = linksProp.getBinaryStream();
        JSONParser parser = new JSONParser();
        
        final URL base = this.urlConstructor.canonicalUrl(resource).setImmutable();
        final AtomicInteger n = new AtomicInteger(0);
        
        try {
            parser.parse(new InputStreamReader(linksStream.getStream()), new JSONDefaultHandler() {

                boolean url = false;

                @Override
                public void endJSON() throws ParseException, IOException {
                    state.complete = true;
                }

                @Override
                public boolean startObjectEntry(String key) throws ParseException, IOException {
                    this.url = "url".equals(key);
                    return true;
                }

                @Override
                public boolean endObjectEntry() throws ParseException, IOException {
                    this.url = false;
                    return true;
                }

                @Override
                public boolean primitive(Object value) throws ParseException, IOException {
                    if (value == null || !this.url) {
                        return true;
                    }

                    if (n.getAndIncrement() < state.index) {
                        return true;
                    }
                    
                    String val = value.toString();
                    if (!shouldCheck(val)) {
                        return true;
                    }
                    LinkCheckResult result = linkChecker.validate(val, base);
                    if (!"OK".equals(result.getStatus())) {
                        state.brokenLinks.add(val);
                    }
                    if (state.brokenLinks.size() >= MAX_BROKEN_LINKS) {
                        return false;
                    }
                    if (n.get()-state.index == MAX_CHECK_LINKS) {
                        return false;
                    }

                    try {
                        checkForInterrupt();
                    } catch (InterruptedException ie) {
                        throw new RuntimeException(ie);
                    }
                    
                    return true;
                }
            });
            state.timestamp = context.getTimestampFormatted();
            state.index = n.get();
            Property result = this.linkCheckPropDef.createProperty();
            state.write(result);
            return result;
        } catch (Throwable t) {
            if (t.getCause() instanceof InterruptedException) {
                throw ((InterruptedException)t.getCause());
            }
            logger.warn("Error checking links for " + resource.getURI(), t);
            return null;
        }
    }

    private static final Pattern SCHEME =
            Pattern.compile("^([a-z][a-z0-9+.-]+):", Pattern.CASE_INSENSITIVE);
    
    private boolean shouldCheck(String href) {
        if (this.blackList != null) {
            for (Pattern p: this.blackList) {
                Matcher m = p.matcher(href);
                if (m.matches()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skip " + href + ": black-listed");
                    }
                    return false;
                }
            }
        }
        Matcher schemeMatcher = SCHEME.matcher(href);
        if (schemeMatcher.find()) {
            String scheme = schemeMatcher.group(1);
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        }
        return ! href.startsWith("#");
    }
    
    private boolean shouldResetState(LinkCheckState state, Resource resource, SystemChangeContext context) {
        if (state.timestamp != null) {
            try {
                final long lastCheckRun = SystemChangeContext.parseTimestamp(state.timestamp).getTime();
                final long resourceLastModified = resource.getLastModified().getTime();
                
                // If linkcheck timestamp is older than resource last modified, 
                // we need to invalidate the link check state.
                if (lastCheckRun < resourceLastModified) return true;

                // If complete and more than MIN_RECHECK_SECONDS between now and last run, do check again.
                if (state.complete) {
                    long now = context.getTimestamp().getTime();
                    if (now - lastCheckRun < MIN_RECHECK_SECONDS*1000) {
                        logger.debug("Not long enough since last completed check (min "
                                + MIN_RECHECK_SECONDS + " seconds, will not reset state).");
                        return false;
                    }
                }
            } catch (java.text.ParseException pe) {
                return true;
            }
        }
        
        return state.complete;
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
    
    public void setBlackList(List<String> blackList) {
        this.blackListConfig = blackList;
        refreshBlackList();
    }
    
    public void setUseRepositoryLocks(boolean useRepositoryLocks) {
        this.useRepositoryLocks = useRepositoryLocks;
    }
    
    public void setUpdateBatch(int updateBatch) {
        this.updateBatch = updateBatch;
    }
    
    public void refreshBlackList() {
        if (this.blackListConfig != null) {
            List<Pattern> blackList = new ArrayList<Pattern>();
            for (String regexp: this.blackListConfig) {
                Pattern p = Pattern.compile(regexp);
                blackList.add(p);
            }
            this.blackList = blackList;
        }
    }
    

    private static class LinkCheckState {
        private List<String> brokenLinks = new ArrayList<String>();
        private int index = 0;
        private String timestamp = null;
        private boolean complete = false;
        
        private LinkCheckState() {}

        @SuppressWarnings("unchecked")
        private static LinkCheckState create(Property statusProp) {
            LinkCheckState s = new LinkCheckState();
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
        
        public void write(Property statusProp) {
            JSONObject obj = toJSONObject();
            try {
                statusProp.setBinaryValue(obj.toJSONString().getBytes("utf-8"), "application/json");
            } catch (UnsupportedEncodingException ex) {
                // Fuck you Java for checked exceptions.
                statusProp.setBinaryValue(obj.toJSONString().getBytes(), "application/json");
            }
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
    
    private static class UpdateBatch {
        private Repository repository;
        private SystemChangeContext context;
        private String token;
        private int batchSize;
        private boolean locking = false;
        private List<Resource> updateList = new ArrayList<Resource>();

        public UpdateBatch(Repository repository, String token, SystemChangeContext context, int batchSize, boolean locking) {
            this.repository = repository;
            this.token = token;
            this.context = context;
            this.batchSize = batchSize;
            this.locking = locking;
        }

        public void add(Resource resource) {
            this.updateList.add(resource);
            if (this.updateList.size() >= this.batchSize) {
                flush();
            }
        }

        public void flush() {
            if (this.updateList.size() > 0) {
                logger.info("Attempting to store " + this.updateList.size() + " resources");
            }
            if (this.locking) {
                flushWithLocking();
                return;
            }
            for (Resource r: this.updateList) {
                try {
                    Resource existing = repository.retrieve(token, r.getURI(), false);
                    if (!existing.getLastModified().equals(r.getLastModified())) {
                        logger.warn("Resource " + r.getURI() + " was modified during link check, skipping store");
                        continue;
                    }
                    repository.store(token, r, context);
                } catch (ResourceLockedException e) {
                    logger.warn("Resource " + r.getURI() + " was locked by another user, skipping");
                    continue;
                } catch (Throwable t) {
                    logger.warn("Unable to store resource " + r, t);
                }
            }
            this.updateList.clear();
        }

        public void flushWithLocking() {
            for (Resource r: this.updateList) {
                Lock lock = null;
                try {
                    Resource resource = repository.lock(token, r.getURI(), context.getJobName(), Depth.ZERO, 60, null);
                    lock = resource.getLock();
                    if (!resource.getLastModified().equals(r.getLastModified())) {
                        logger.warn("Resource " + r.getURI() + " was modified during link check, skipping store");
                        continue;
                    }
                    repository.store(token, resource, context);
                } catch (ResourceLockedException e) {
                    logger.warn("Resource " + r.getURI() + " was locked by another user, skipping");
                    continue;
                } catch (Throwable t) {
                    logger.warn("Unable to store resource " + r, t);
                } finally {
                    if (lock != null) {
                        try {
                            repository.unlock(token, r.getURI(), lock.getLockToken());
                        } catch (Exception e) {
                            logger.warn("Unable to unlock resource " + r.getURI(), e);
                        }
                    }
                }
            }
            this.updateList.clear();
        }
    }
    
}
