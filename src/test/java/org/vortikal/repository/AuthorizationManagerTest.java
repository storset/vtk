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
package org.vortikal.repository;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.GroupStore;
import org.vortikal.security.MatchingGroupStore;
import org.vortikal.security.MatchingPrincipalStore;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalImpl;
import org.vortikal.security.PrincipalManager;
import org.vortikal.security.PrincipalManagerImpl;
import org.vortikal.security.roles.RoleManager;

public class AuthorizationManagerTest {

    @Test
    public void test01() throws Exception {
        TestHarness harness = new TestHarness("auth-manager-test01.json");
        harness.run();
    }

    private static class TestHarness {
        
        private TestDataAccessor dao;
        private PrincipalManager principalManager;
        private AuthorizationManager authorizationManager;
        private List<TestAssertion> assertions;
        
        public TestHarness(String classPathRef) throws Exception {
            init(classPathRef);
        }
        
        public void run() throws Exception {
            for (TestAssertion assertion: this.assertions) {
                assertion.test(this.authorizationManager);
            }
        }
        
        private void init(String classPathRef) throws Exception {
            InputStream in = getClass().getResourceAsStream(classPathRef);
            if (in == null) {
                throw new IllegalStateException("File '" + classPathRef + "' cannot be found");
            }
            Reader reader = new InputStreamReader(in);
            JSONObject toplevel = (JSONObject) JSONValue.parse(reader);
            
            JSONObject principalManager = (JSONObject) toplevel.get("principal-manager");
            initPrincipalManager(principalManager.toJSONString());
            
            JSONObject repo = (JSONObject) toplevel.get("resources");
            
            List<Path> list = new ArrayList<Path>();
            for (Object key: repo.keySet()) {
                list.add(Path.fromString(key.toString()));
            }
            Collections.sort(list, new Comparator<Path>() {
                @Override
                public int compare(Path arg0, Path arg1) {
                    if (arg0.isAncestorOf(arg1)) {
                        return -1;
                    } else if (arg0.equals(arg1)) {
                        return 0;
                    } else {
                        return 1;
                    }
                }
            });
            StringBuilder sb = new StringBuilder("{");
            for (Path key: list) {
                sb.append('"').append(key).append('"').append(" : ");
                sb.append(((JSONObject) repo.get(key.toString())).toJSONString()); 
            }
            sb.append("}");
            
            initDataAccessor(sb.toString());
            
            initAuthManager();
            
            JSONObject assertions = (JSONObject) toplevel.get("assertions");
            initAssertions(assertions.toJSONString());

        }

        private void initPrincipalManager(String json) throws Exception {
            JSONObject obj = (JSONObject) JSONValue.parse(json);

            JSONObject principalStoreJson = (JSONObject) obj.get("matching-principal-store");
            String users = principalStoreJson.get("pattern").toString();
            
            JSONObject groupStoreJson = (JSONObject) obj.get("matching-group-store");
            @SuppressWarnings("unchecked")
            Map<String, String> groups = 
                    (Map<String, String>) groupStoreJson.get("map");
            
            
            MatchingPrincipalStore ps = new MatchingPrincipalStore();
            ps.setPattern(users);
            
            MatchingGroupStore gs = new MatchingGroupStore();
            gs.setGroupsMap(groups);
            
            PrincipalManagerImpl pm = new PrincipalManagerImpl();
            pm.setPrincipalStore(ps);
            pm.setGroupStores(Arrays.asList(new GroupStore[]{gs}));
            
            this.principalManager = pm;
        }
        

        private void initDataAccessor(String json) throws Exception {
            Reader reader = new StringReader(json);
            TestResourceFactory factory = new TestResourceFactory();
            final TestDataAccessor dao = new TestDataAccessor();

            factory.load(reader, new TestResourceFactory.Consumer() {
                @Override
                public boolean resource(ResourceImpl resource) {
                    dao.put(resource);
                    return true;
                }
                @Override
                public void end() {
                }
            });
            System.out.println(dao.load(Path.ROOT).getAcl());
            this.dao = dao;
        }
        
        private void initAuthManager() {
            AuthorizationManager mgr = new AuthorizationManager();
            mgr.setDao(dao);
            mgr.setPrincipalManager(this.principalManager);
            mgr.setRoleManager(new RoleManager());
            this.authorizationManager = mgr;
        }
        
        private void initAssertions(String json) throws Exception {
            JSONObject object = (JSONObject) JSONValue.parse(json);
            List<TestAssertion> assertions = new ArrayList<TestAssertion>();
            for (Object key: object.keySet()) {
                JSONObject val = (JSONObject) object.get(key);
                requireFields(val, "uri", "principal", "repository-action", "expected-outcome");
                Path uri = Path.fromString(val.get("uri").toString());
                Object user = val.get("principal");
                Principal principal = user != null ? 
                        new PrincipalImpl(user.toString(), Principal.Type.USER) : null;
                
                if (principal != null && !principalManager.validatePrincipal(principal)) {
                    throw new IllegalArgumentException("Invalid principal: " + principal);
                }

                RepositoryAction action = RepositoryAction.valueOf(
                        val.get("repository-action").toString());
                Outcome outcome = Outcome.valueOf(val.get("expected-outcome").toString());
                TestAssertion assertion = new TestAssertion(
                        key.toString(), uri, principal, action, outcome);
                assertions.add(assertion);
            }
            this.assertions = assertions;
        }

        private static enum Outcome {
            SUCCESS, FAILURE;
        }
        
        private static class TestAssertion {
            private String id;
            private Path uri;
            private Principal principal;
            private RepositoryAction action;
            private Outcome outcome;
            public TestAssertion(String id, Path uri, Principal principal,
                    RepositoryAction action, Outcome outcome) {
                this.id = id;
                this.uri = uri;
                this.principal = principal;
                this.action = action;
                this.outcome = outcome;
            }
            
            public void test(AuthorizationManager authManager) throws Exception {
                switch (this.outcome) {
                case SUCCESS:
                    try {
                        authManager.authorizeAction(this.uri, this.action, this.principal);
                        return;
                    } catch (Throwable t) {
                        throw new Exception("Assertion " + this + " failed", t);
                    }
                case FAILURE:
                    try {
                        authManager.authorizeAction(this.uri, this.action, this.principal);
                    } catch (Throwable t) {
                        return;
                    }
                    throw new Exception("Assertion " + this + " should have failed");
                    
                default:
                    throw new IllegalStateException("Undefined outcome: " + outcome);
                }
            }
            
            public String toString() {
                return this.id;
            }
        }
    }
    
    private static class TestDataAccessor implements DataAccessor {

        private Map<Path, ResourceImpl> tree = new HashMap<Path, ResourceImpl>();
        
        public void put(ResourceImpl resource) {
            if (resource == null) {
                throw new IllegalArgumentException("Resource is NULL");
            }
            if (resource.getAcl() == null && resource.getURI() == Path.ROOT) {
                throw new IllegalArgumentException("Root resource must have an ACL");
            }
            if (resource.getURI() != Path.ROOT) {
                for (Path p: resource.getURI().getAncestors()) {
                    if (!this.tree.containsKey(p)) {
                        throw new IllegalArgumentException("Cannot add '" + resource.getURI() 
                                + "': '" + p + "' does not exist");
                    }
                }
                ResourceImpl parent = this.tree.get(resource.getURI().getParent());
                parent.addChildURI(resource.getURI());
            }
            
            this.tree.put(resource.getURI(), resource);
        }
        
        @Override
        public ResourceImpl load(Path uri) throws DataAccessException {
            if (!tree.containsKey(uri)) {
                return null;
            }
            
            ResourceImpl r = null;
            Acl acl = null;
            for (Path p: uri.getPaths()) {
                r = this.tree.get(p);
                if (!r.isInheritedAcl()) {
                    acl = r.getAcl();
                }
            }
            try {
                ResourceImpl clone = (ResourceImpl) r.clone();
                clone.setAcl(acl);
                return clone;
            } catch (Throwable t) { 
                return null;
            }
        }
        
        @Override
        public ResourceImpl[] loadChildren(ResourceImpl parent)
                throws DataAccessException {
            List<ResourceImpl> children = new ArrayList<ResourceImpl>();
            for (Path childURI: parent.getChildURIs()) {
                ResourceImpl child = load(childURI);
                if (child == null) {
                    throw new IllegalStateException("Inconsistent state: child " 
                            + childURI + " does not exist");
                }
                children.add(child);
            }
            return children.toArray(new ResourceImpl[children.size()]);
        }

        @Override
        public ResourceImpl store(ResourceImpl r) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceImpl storeACL(ResourceImpl r) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceImpl storeLock(ResourceImpl r)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(ResourceImpl resource) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markDeleted(ResourceImpl resource, ResourceImpl parent,
                Principal principal, String trashID) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecoverableResource> getRecoverableResources(
                int parentResourceId) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceImpl recover(Path parentUri,
                RecoverableResource recoverableResource)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteRecoverable(RecoverableResource recoverableResource)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecoverableResource> getTrashCanOverdue(int overdueLimit)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecoverableResource> getTrashCanOrphans()
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteExpiredLocks(Date expireDate)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceImpl copy(ResourceImpl resource,
                ResourceImpl destParent, PropertySet newResource,
                boolean copyACLs, PropertySet fixedProperties)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceImpl move(ResourceImpl resource, ResourceImpl newResource)
                throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path[] discoverLocks(Path uri) throws DataAccessException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path[] discoverACLs(Path uri) throws DataAccessException {
            List<Path> result = new ArrayList<Path>();
            for (Path p: this.tree.keySet()) {
                if (uri.isAncestorOf(p) || uri.equals(p)) {
                    ResourceImpl r = this.tree.get(p);
                    if (r.getAcl() != null) {
                        result.add(p);
                    }
                }
            }
            return result.toArray(new Path[result.size()]);
        }

        @Override
        public Set<Principal> discoverGroups() throws DataAccessException {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public boolean validate() throws DataAccessException {
            throw new UnsupportedOperationException();
        }

    }
    
    private static void requireFields(JSONObject obj, String...fields) {
        for (String field: fields) {
            if (!obj.containsKey(field)) {
                throw new IllegalArgumentException("Required field '" + field + "' not in object " + obj);
            }
        }
    }
}
