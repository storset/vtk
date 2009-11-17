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
package org.vortikal.repository.store.jcr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AclImpl;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.Comment;
import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.store.CommentDAO;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.SecurityContext;
import org.vortikal.security.Principal.Type;

public class JcrDao implements ContentStore, DataAccessor, CommentDAO, InitializingBean, DisposableBean {

    private static final Log logger = LogFactory.getLog(JcrDao.class);

    protected Repository repository;
    private ResourceTypeTree resourceTypeTree;
    
    private PrincipalFactory principalFactory;
    
    private org.springframework.core.io.Resource nodeTypesDefinition;
    
    // Session keeping repo alive (and used during initialization)
    Session systemSession;

    public void copy(ResourceImpl resource, ResourceImpl dest,
            PropertySet newResource, boolean copyACLs,
            PropertySet fixedProperties) throws DataAccessException {

        Session session = getSession();

        String path = JcrPathUtil.uriToPath(resource.getURI().toString());
        String newPath = JcrPathUtil.uriToPath(newResource.getURI().toString());

        try {
            Workspace workspace = session.getWorkspace();
            workspace.copy(path, newPath);
            
            // Make sure we have any open scoped lock tokens available
            // in our current session.
            Node newResourceNode = (Node)session.getItem(newPath);
            acquireLockToken(session, newResourceNode);
            
            // Update resource name property.
            setResourceNameProperty(newResourceNode, newResource.getURI());

            if (!copyACLs) {
                Path[] acls = discoverACLs(newResource.getURI());
                for (Path resourceWithAclUri : acls) {
                    String resourceWithAclPath = JcrPathUtil.uriToPath(resourceWithAclUri.toString());
                    
                    Node node = (Node) session.getItem(resourceWithAclPath);
                    if (node.hasNode(JcrDaoConstants.VRTX_ACL_NAME)) {
                        node.getNode(JcrDaoConstants.VRTX_ACL_NAME).remove();
                    }
                }
            }

            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }


    public void delete(ResourceImpl resource) throws DataAccessException {

        Session session = getSession();
        String jcrPath = JcrPathUtil.uriToPath(resource.getURI().toString());
        try {
            Node node = (Node)session.getItem(jcrPath);
            if (node.isLocked()) {
                acquireLockToken(session, node);
                unlock(session, node);
            }
            node.remove();
            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }

    }

    
    private void acquireLockToken(Session session, Node node)
            throws RepositoryException {
        if (node.isLocked()) {
            synchronized (this.systemSession) {
                Node lockNode = (Node) session.getItem(JcrDaoConstants.LOCKS_ROOT);  
                lockNode = lockNode.getNode(node.getLock().getNode().getUUID());
                //if (lockNode.getProperty(VRTX_PREFIX + "owner").getString().equals(session.getUserID()))
                    session.addLockToken(lockNode.getProperty(JcrDaoConstants.VRTX_PREFIX + "jcrLockToken").getString());
            }
        }
    }

    public ResourceImpl load(Path uri) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());

        try {
            Node node = (Node) session.getItem(path);
            return nodeToResource(node);
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public PropertySet load(Path uri, String revision) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());

        try {
            Node node = (Node) session.getItem(path);
            VersionHistory vh = node.getVersionHistory();
            Version v = vh.getVersion(revision);
            PropertySet r = versionToResource(v, uri);
            return r;
        } catch (PathNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    private List<Path> getChildUris(Node node) throws RepositoryException {
        List<Path> children = new ArrayList<Path>();

        for (NodeIterator i = node.getNodes(); i.hasNext();) {
            Node child = i.nextNode();
            if (child.getName().equals(JcrDaoConstants.VRTX_ACL_NAME)) {
                continue;
            }
            String uri = JcrPathUtil.pathToUri(child.getPath());
            children.add(Path.fromString(uri));
        }
        return children;
    }

    private PropertySet versionToResource(Version v, Path uri) throws RepositoryException {
        ResourceImpl resource = createResourceImpl();
        resource.setUri(uri);
        setProperties(resource, v.getNode("jcr:frozenNode"));
        return resource;
    }
    
    // XXX: made public
    public ResourceImpl nodeToResource(Node node) throws RepositoryException {
        Path uri = Path.fromString(JcrPathUtil.pathToUri(node.getPath()));

        ResourceImpl resource = createResourceImpl();
        resource.setUri(uri);
        setProperties(resource, node);
        resource.setAcl(getAcl(node, resource));

        return resource;
    }

    private void setProperties(ResourceImpl resource, Node node) throws RepositoryException {
        String resourceType = node.getProperty(JcrDaoConstants.RESOURCE_TYPE).getString();
        if (logger.isTraceEnabled()) logger.trace("Prop: " + JcrDaoConstants.RESOURCE_TYPE + " value: " + resourceType);

        resource.setResourceType(resourceType);

        for (PropertyIterator properties = node.getProperties(JcrDaoConstants.VRTX_PREFIX + "*"); properties.hasNext();) {
            Property prop = properties.nextProperty();
            
            String name = prop.getName();

            if (name.equals(JcrDaoConstants.RESOURCE_TYPE) 
                   || name.equals(JcrDaoConstants.CONTENT)
                   || name.equals(JcrDaoConstants.RESOURCE_NAME)) {
                continue;
            }

            name = name.substring(JcrDaoConstants.VRTX_PREFIX.length());

            String prefix = null;
            int sepIndex = name.indexOf(JcrDaoConstants.VRTX_PREFIX_SEPARATOR);
            if (sepIndex != -1) {
                prefix = name.substring(0, sepIndex);
                name = name.substring(sepIndex + 1);
            }

            List<String> stringValues = new ArrayList<String>();

            if (prop.getDefinition().isMultiple()) {
                for (Value value : prop.getValues()) {
                    stringValues.add(value.getString());
                }
            } else {
                stringValues.add(prop.getString());
            }
            if (logger.isTraceEnabled()) logger.trace("Prop: " + name + " values: " + stringValues);
            org.vortikal.repository.Property p = createProperty(prefix, name, stringValues);
            resource.addProperty(p);
        }
        
        if (node.isLocked()) {
            Lock lock = node.getLock();
            resource.setLock(getLock(lock, node.getSession()));
        }

        if (resource.isCollection()) {
            resource.setChildURIs(getChildUris(node));
        }
    }

    private org.vortikal.repository.Property createProperty(String prefix, String name, List<String> stringValues) {
        PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyDefinitionByPrefix(prefix, name);
        org.vortikal.repository.Property prop = propDef.createProperty(stringValues.toArray(new String[stringValues.size()]));
        return prop;
    }
    
    private AclImpl getAcl(Node node, ResourceImpl resource)
            throws RepositoryException, ItemNotFoundException,
            AccessDeniedException, UnsupportedRepositoryOperationException,
            ValueFormatException, PathNotFoundException {
        AclImpl acl = new AclImpl();
        Node aclNode = getAclNode(node);
        
        Node parent = aclNode.getParent();
        if (parent.isSame(node)) {
            resource.setAclInheritedFrom(-1);
        } else {
            // XXX: Ok, this isn't all right
            resource.setAclInheritedFrom(parent.getUUID().hashCode());
        }
        
        for (NodeIterator i = aclNode.getNodes(JcrDaoConstants.VRTX_PREFIX + "*"); i.hasNext();) {
            Node actionNode = i.nextNode();
            RepositoryAction action = Privilege.getActionByName(actionNode.getName().substring(JcrDaoConstants.VRTX_PREFIX.length()));
            for (NodeIterator i2 = actionNode.getNodes(JcrDaoConstants.VRTX_PREFIX + "*"); i2.hasNext();) {
                Node principalNode = i2.nextNode();
                String name = JcrPathUtil.unescapeIllegalJcrChars(principalNode.getName().substring(JcrDaoConstants.VRTX_PREFIX.length()));
                Type type = Principal.Type.valueOf(principalNode.getProperty(JcrDaoConstants.VRTX_PRINCIPAL_TYPE_NAME).getString());

                Principal principal = null;
                if (type == Principal.Type.GROUP) {
                    principal = principalFactory.getPrincipal(name, Type.GROUP);
                } else if (type == Principal.Type.PSEUDO) {
                    principal = principalFactory.getPrincipal(name, Type.PSEUDO);
                } else {
                    principal = principalFactory.getPrincipal(name, Type.USER);
                }
                if (logger.isTraceEnabled()) logger.trace("** Adding to acl: " + action + " - " + name);
                acl.addEntry(action, principal);
            }
        }
        return acl;
    }

    private Node getAclNode(Node node) throws RepositoryException {
        try {
            return node.getNode(JcrDaoConstants.VRTX_ACL_NAME);
        } catch (PathNotFoundException e) {
            return getAclNode(node.getParent());
        }
    }

    private org.vortikal.repository.Lock getLock(Lock lock, Session session) throws PathNotFoundException, RepositoryException {
        Node lockNode = (Node) session.getItem(JcrDaoConstants.LOCKS_ROOT);
        String uuid = lock.getNode().getUUID();
        Node node = lockNode.getNode(uuid);
        String token = node.getProperty(JcrDaoConstants.VRTX_PREFIX + "lockToken").getString();
        String ownerInfo = node.getProperty(JcrDaoConstants.VRTX_PREFIX + "ownerInfo").getString();
        String owner = node.getProperty(JcrDaoConstants.VRTX_PREFIX + "owner").getString();
        String depthStr = node.getProperty(JcrDaoConstants.VRTX_PREFIX + "depth").getString();
        Date timeOut = new Date(Long.parseLong(node.getProperty(JcrDaoConstants.VRTX_PREFIX + "timeOut").getString()));

        Depth depth = Depth.fromString(depthStr);
        
        if (logger.isTraceEnabled()) logger.trace("Building lock for '" + uuid + "': " + node.getProperty(JcrDaoConstants.VRTX_PREFIX + "owner").getString() + ", " + token);
        org.vortikal.repository.LockImpl vLock = new org.vortikal.repository.LockImpl(
                token, principalFactory.getPrincipal(owner, Principal.Type.USER), ownerInfo, depth, timeOut);

        return vLock;
    }

    /**
     * This method needs to be overridden by the framework.
     * 
     */
    protected ResourceImpl createResourceImpl() {
        throw new DataAccessException("The method createResourceImpl() needs to be overridden.");
    }

    public ResourceImpl[] loadChildren(ResourceImpl parent)
            throws DataAccessException {
        Session session = getSession();

        String parentPath = JcrPathUtil.uriToPath(parent.getURI().toString());

        try {
            List<ResourceImpl> children = new ArrayList<ResourceImpl>();

            Node node = (Node) session.getItem(parentPath);
            
            for (NodeIterator i = node.getNodes(); i.hasNext();) {
                Node child = i.nextNode();
                if (child.getName().equals(JcrDaoConstants.VRTX_ACL_NAME)) {
                    continue;
                }
                children.add(nodeToResource(child));
            }
            return children.toArray(new ResourceImpl[children.size()]);
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void move(ResourceImpl resource, ResourceImpl newResource)
            throws DataAccessException {

        Session session = getSession();

        String path = JcrPathUtil.uriToPath(resource.getURI().toString());
        String newPath = JcrPathUtil.uriToPath(newResource.getURI().toString());

        try {
            Workspace workspace = session.getWorkspace();
            workspace.move(path, newPath);
            
            // Need any open-scoped lock token available in session.
            Node movedNode = (Node)session.getItem(newPath);
            acquireLockToken(session, movedNode);
            // Update resource name property.
            setResourceNameProperty(movedNode, 
                                    newResource.getURI());
            
            // Session must be explicitly saved because of resource name property update.
            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }

    }

    public void create(ResourceImpl resource) {
        Session session = getSession();

        String parentPath = JcrPathUtil.uriToPath(resource.getURI().getParent().toString());

        try {
            Node node = (Node) session.getItem(parentPath);
            acquireLockToken(session, node);

            String primaryResourceType = (resource.isCollection()) ? JcrDaoConstants.VRTX_FOLDER_NAME : JcrDaoConstants.VRTX_FILE_NAME;
            node = node.addNode(JcrPathUtil.escapeIllegalJcrChars(resource.getName()), primaryResourceType);

            for (org.vortikal.repository.Property prop : resource.getProperties()) {
                String propName = jcrPropName(prop);
                
                if (prop.getDefinition().isMultiple()) {
                    List<String> nativeValues = new ArrayList<String>();
                    for (org.vortikal.repository.resourcetype.Value value : prop.getValues()) {
                        nativeValues.add(value.getNativeStringRepresentation());
                    }
                    node.setProperty(propName, nativeValues.toArray(new String[nativeValues.size()]));
                } else {
                    node.setProperty(propName, prop.getValue().getNativeStringRepresentation());
                }
            }

            node.setProperty(JcrDaoConstants.RESOURCE_TYPE, resource.getResourceType());
            setResourceNameProperty(node, resource.getURI());

            if (!resource.isCollection()) {
                node.setProperty(JcrDaoConstants.CONTENT, new ByteArrayInputStream(new byte[0]));
            }
            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }

    }

    private String jcrPropName(org.vortikal.repository.Property prop) {
        String propName = prop.getDefinition().getName();
        String prefix = prop.getDefinition().getNamespace().getPrefix();
        if (prefix != null) {
            propName = prefix + JcrDaoConstants.VRTX_PREFIX_SEPARATOR + propName;
        }
        propName = JcrDaoConstants.VRTX_PREFIX + propName;
        return propName;
    }

    // Turn around
    public Path[] discoverLocks(Path uri) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());

        try {

            Node node = (Node) session.getItem(path);
            if (!node.isLocked()) {
                return new Path[0];
            }
            
            Path lockUri = Path.fromString(JcrPathUtil.pathToUri(node.getLock().getNode().getPath()));
            return new Path[] { lockUri };
        } catch (PathNotFoundException e) {
            return new Path[0];
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void store(ResourceImpl r) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(r.getURI().toString());

        try {
            Node node = null;
            try {
                node = (Node) session.getItem(path);
//                if (!node.isCheckedOut()) {
//                    node.checkout();
//                }
            } catch (PathNotFoundException e) {
                create(r);
                return;
            }
            acquireLockToken(session, node);

            // Lock handling currently requires 0 depth...
            org.vortikal.repository.Lock lock = r.getLock();
            if (lock != null && !node.holdsLock()) {
                lock(session, node, lock);
                return;
            } else if (lock == null && node.holdsLock()) {
                unlock(session, node);
                return;
            }

            List<String> vProps = new ArrayList<String>();

            for (org.vortikal.repository.Property prop : r.getProperties()) {
                String propName = jcrPropName(prop);
                if (prop.getDefinition().isMultiple()) {
                    List<String> nativeValues = new ArrayList<String>();
                    for (org.vortikal.repository.resourcetype.Value value : prop.getValues()) {
                        nativeValues.add(value.getNativeStringRepresentation());
                    }
                    node.setProperty(propName, nativeValues.toArray(new String[nativeValues.size()]));
                } else {
                    node.setProperty(propName, prop.getValue().getNativeStringRepresentation());
                }
                vProps.add(propName);
            }

            List<Property> toRemove = new ArrayList<Property>();

            for (PropertyIterator i = node.getProperties(JcrDaoConstants.VRTX_PREFIX + "*"); i.hasNext();) {
                Property p = i.nextProperty();

                String name = p.getName();
                if (!name.equals(JcrDaoConstants.CONTENT) && !vProps.contains(name)) {
                    toRemove.add(p);
                }
            }

            for (Property property : toRemove) {
                if (!property.getDefinition().isProtected()) {
                    property.remove();
                }
            }

            node.setProperty(JcrDaoConstants.RESOURCE_TYPE, r.getResourceType());
            
            // Add resource name explicitly for searchability (not used when mapping nodes to resources)
            // jcr:name is not suitable for this.
            setResourceNameProperty(node, r.getURI());

            session.save();
//            if (node.isCheckedOut() || node.isNodeType("mix:versionable")) {
//                node.checkin();
//            }
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }
    
    private void setResourceNameProperty(Node resourceNode, Path uri) 
        throws RepositoryException {
        resourceNode.setProperty(JcrDaoConstants.RESOURCE_NAME, uri.getName());
    }

    private void unlock(Session session, Node node) throws RepositoryException {
        node.unlock();
        synchronized (this.systemSession) {
            Node lockNode = (Node) session.getItem(JcrDaoConstants.LOCKS_ROOT);
            Node node2 = lockNode.getNode(node.getUUID());
            if (logger.isDebugEnabled()) logger.debug("Unlocking '" + node.getPath() + "': " + node2.getProperty(JcrDaoConstants.VRTX_PREFIX + "owner").getString());
            node2.remove();
            session.save();
        }
    }

    private void lock(Session session, Node node, org.vortikal.repository.Lock lock)
            throws UnsupportedRepositoryOperationException, LockException,
            AccessDeniedException, InvalidItemStateException,
            RepositoryException, PathNotFoundException, ItemExistsException,
            VersionException, ConstraintViolationException,
            ValueFormatException, NoSuchNodeTypeException {

        node.lock(false, false);

        if (logger.isDebugEnabled()) logger.debug("Locking '" + node.getPath() + "', '"+ node.getUUID()+ "': " + node.getLock().getLockToken() + ", " + session.getUserID());

        synchronized (this.systemSession) {
            Node lockNode = (Node) session.getItem(JcrDaoConstants.LOCKS_ROOT);
            lockNode = lockNode.addNode(node.getUUID(), JcrDaoConstants.VRTX_LOCK_NAME);
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "jcrLockToken", node.getLock().getLockToken());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "lockToken", lock.getLockToken());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "depth", lock.getDepth().toString());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "ownerInfo", lock.getOwnerInfo());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "owner", lock.getPrincipal().getQualifiedName());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "timeOut", lock.getTimeout().getTime());
            lockNode.setProperty(JcrDaoConstants.VRTX_PREFIX + "nodeRef", node);
            session.save();
        }
    }

    public void storeContent(Path uri, InputStream byteStream) {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());
        
        try {
            Node node = (Node) session.getItem(path);
            acquireLockToken(session, node);
            node.checkout();
            node.setProperty(JcrDaoConstants.CONTENT, byteStream);
            session.save();
//            node.checkin();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void storeACL(ResourceImpl r) throws DataAccessException {
        Session session = getSession();
        Acl newAcl = r.getAcl();
        boolean isInherited = r.isInheritedAcl();
        try {
            Node node = (Node)session.getItem(JcrPathUtil.uriToPath(r.getURI().toString()));
            Node aclNode = null;

            try {
                aclNode = node.getNode(JcrDaoConstants.VRTX_ACL_NAME);
            } catch (PathNotFoundException e) {
                if (isInherited) {
                    // Was inherited, is inherited..
                    return;
                }
            }

            if (aclNode != null) {
                aclNode.remove();
            }
            
            if (isInherited) {
                // Is inherited, wasn't before
                session.save();
                return;
            }
            
            aclNode = node.addNode(JcrDaoConstants.VRTX_ACL_NAME, JcrDaoConstants.VRTX_ACL_NAME);

            for (RepositoryAction action : newAcl.getActions()) {
                Node actionNode = aclNode.addNode(JcrDaoConstants.VRTX_PREFIX + action.toString(), JcrDaoConstants.VRTX_ACTION_NAME);
                for (Principal principal : newAcl.getPrincipalSet(action)) {
                    Node principalNode = actionNode.addNode(JcrDaoConstants.VRTX_PREFIX 
                            + JcrPathUtil.escapeIllegalJcrChars(principal.getQualifiedName()), JcrDaoConstants.VRTX_PRINCIPAL_NAME);
                    principalNode.setProperty(JcrDaoConstants.VRTX_PRINCIPAL_TYPE_NAME, principal.getType().name());
                }
            }
            session.save();
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public boolean validate() throws DataAccessException {
        return true;
    }

    public void addChangeLogEntry(ChangeLogEntry entry, @SuppressWarnings("unused") boolean recurse)
            throws DataAccessException {
        logger.info("## Nicely formatted change log entry for "
                + entry.getOperation().name() + " on " + entry.getUri());

    }

    public void deleteExpiredLocks(Date d) throws DataAccessException {
        Session session = getSystemSession();
        
        try {
            StringBuilder stmt = new StringBuilder();
            stmt.append("select * from vrtx:lock where vrtx:timeOut < ");
            stmt.append(d.getTime());
            Query query = session.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.SQL); 
            QueryResult result = query.execute();

            NodeIterator lockNodes = result.getNodes();
            while (lockNodes.hasNext()) {
                
                Node lockNode = lockNodes.nextNode();
                Property nodeRef = lockNode.getProperty("vrtx:nodeRef");
                Node node = nodeRef.getNode();
                if (logger.isDebugEnabled()) {
                    logger.debug("Lock expired: " + JcrPathUtil.pathToUri(node.getPath()));
                }
                acquireLockToken(session, node);
                unlock(session, node);
            }
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public Path[] discoverACLs(Path uri) throws DataAccessException {
        Session session = getSession();
        
        try {
            StringBuilder stmt = new StringBuilder();
            stmt.append("select * from vrtx:acl where jcr:path like '");
            stmt.append(JcrDaoConstants.VRTX_ROOT);
            stmt.append(uri.toString());
            if (!uri.isRoot()) {
                stmt.append("/");
            }
            stmt.append("%'");
            
            Query query = session.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.SQL); 
            QueryResult result = query.execute();

            NodeIterator nodes = result.getNodes();
            List<Path> resultList = new ArrayList<Path>();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                Path aclNode = Path.fromString(JcrPathUtil.pathToUri(node.getParent().getPath()));
                resultList.add(aclNode);
            }
            return resultList.toArray(new Path[resultList.size()]);
            
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public Set<Principal> discoverGroups() throws DataAccessException {
        return new HashSet<Principal>();
    }

    // Content store:

    public void copy(Path srcURI, Path destURI) throws DataAccessException {
        // ContentStore.copy() ignored
    }

    public void createResource(Path uri, boolean isCollection)
            throws DataAccessException {
        // ContentStore.createResource() ignored
    }

    public void deleteResource(Path uri) throws DataAccessException {
        // ContentStore.deleteResource() ignored
    }

    public long getContentLength(Path uri) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());
        
        try {
            Node node = (Node) session.getItem(path);
            return node.getProperty(JcrDaoConstants.CONTENT).getLength();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public InputStream getInputStream(Path uri) throws DataAccessException {
        Session session = getSession();

        String path = JcrPathUtil.uriToPath(uri.toString());

        try {
            Node node = (Node) session.getItem(path);
            return node.getProperty(JcrDaoConstants.CONTENT).getStream();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void move(Path srcURI, Path destURI) throws DataAccessException {
        // ContentStore.move() ignored
    }


    // CommentDAO implementation
    
    public Comment createComment(Comment comment) {
    	return null;
    }

    public Comment createComment(Resource resource, Comment comment) throws RuntimeException {
        Session session = getSession();
        try {
            Node resourceNode = (Node)session.getItem(JcrPathUtil.uriToPath(resource.getURI().toString()));
            Node commentsNode = null;
            if (resourceNode.hasNode(JcrDaoConstants.VRTX_COMMENTS_NAME)) {
                commentsNode = resourceNode.getNode(JcrDaoConstants.VRTX_COMMENTS_NAME);
            } else {
                commentsNode = resourceNode.addNode(JcrDaoConstants.VRTX_COMMENTS_NAME, JcrDaoConstants.VRTX_COMMENTS_NAME);
            }
            int id = 0;
            for (NodeIterator i = commentsNode.getNodes(); i.hasNext(); i.next()) id++;
            Node commentNode = commentsNode.addNode(String.valueOf(id), JcrDaoConstants.VRTX_COMMENT_NAME);
            populateCommentNode(commentNode, comment);

            session.save();
            return nodeToComment(commentNode);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void deleteComment(Comment comment) throws RuntimeException {
        Session session = getSession();
        try {
            Node resourceNode = (Node) session.getItem(JcrPathUtil.uriToPath(comment.getURI().toString()));
            Node commentsNode = resourceNode.getNode(JcrDaoConstants.VRTX_COMMENTS_NAME);
            Node commentNode = commentsNode.getNode(comment.getID());
            commentNode.remove();
            session.save();
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public Comment updateComment(Comment comment) throws RuntimeException {
        Session session = getSession();
        try {
            Node resourceNode = (Node) session.getItem(JcrPathUtil.uriToPath(comment.getURI().toString()));
            Node commentsNode = resourceNode.getNode(JcrDaoConstants.VRTX_COMMENTS_NAME);
            Node commentNode = commentsNode.getNode(comment.getID());

            PropertyIterator props = commentNode.getProperties();
            while (props.hasNext()) {
                props.remove();
            }
            populateCommentNode(commentNode, comment);
            session.save();
            return nodeToComment(commentNode);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }
    
    public void deleteAllComments(Resource resource) throws RuntimeException {
        Session session = getSession();
        try {
            Node resourceNode = (Node) session.getItem(JcrPathUtil.uriToPath(resource.getURI().toString()));
            Node commentsNode = resourceNode.getNode(JcrDaoConstants.VRTX_COMMENTS_NAME);
            commentsNode.remove();
            session.save();
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    
    public List<Comment> listCommentsByResource(Resource resource,
            boolean deep, int max) throws RuntimeException {
        List<Comment> resultList = new ArrayList<Comment>();
        Session session = getSession();
        try {
            Path uri = resource.getURI();
            String jcrPath = JcrDaoConstants.VRTX_ROOT;
            if (!"/".equals(uri)) {
                jcrPath += uri;
            }
            StringBuilder stmt = new StringBuilder();
            stmt.append("select * from vrtx:comment ");
            if (deep) {
                stmt.append("where jcr:path like '").append(jcrPath).append("/%' ");
            } else {
                stmt.append("where jcr:path = '").append(jcrPath);
                stmt.append("/").append(JcrDaoConstants.VRTX_COMMENTS_NAME);
                stmt.append("/%' ");
            }
            stmt.append("order by vrtx:commentTime");
            Query query = session.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.SQL); 
            QueryResult result = query.execute();

            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext() && resultList.size() < max) {
                Comment c = nodeToComment((Node)nodes.next());
                resultList.add(0, c);
            }
            return resultList;
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public int getNumberOfComments(Resource resource) throws RuntimeException {
        Session session = getSession();
        try {
            Path uri = resource.getURI();
            String jcrPath = JcrDaoConstants.VRTX_ROOT;
            if (!"/".equals(uri)) {
                jcrPath += uri;
            }
            StringBuilder stmt = new StringBuilder();
            stmt.append("select * from vrtx:comment ");
            stmt.append("where jcr:path = '").append(jcrPath);
            stmt.append("/").append(JcrDaoConstants.VRTX_COMMENTS_NAME);
            stmt.append("/%' ");
            stmt.append("order by vrtx:commentTime");
            Query query = session.getWorkspace().getQueryManager().createQuery(stmt.toString(), Query.SQL); 
            QueryResult result = query.execute();
            long comments = result.getNodes().getSize();
            return (int) comments;
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }
    private void populateCommentNode(Node commentNode, Comment comment) throws RepositoryException {
        String author = comment.getAuthor().getQualifiedName();
        String content = comment.getContent();
        Date time = comment.getTime();
        String title = comment.getTitle();
        commentNode.setProperty(JcrDaoConstants.VRTX_COMMENT_AUTHOR, author);
        if (title != null) {
            commentNode.setProperty(JcrDaoConstants.VRTX_COMMENT_TITLE, title);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        commentNode.setProperty(JcrDaoConstants.VRTX_COMMENT_TIME, cal);
        try {
            commentNode.setProperty(JcrDaoConstants.VRTX_COMMENT_BODY, new ByteArrayInputStream(
                    content.getBytes("utf-8")));
        } catch (UnsupportedEncodingException e) { }            
        
    }

    private Comment nodeToComment(Node node) throws RepositoryException {
        Comment c = new Comment();
        c.setID(node.getName());
        Principal author = this.principalFactory.getPrincipal(node.getProperty(JcrDaoConstants.VRTX_COMMENT_AUTHOR).getString(), Principal.Type.USER);
        c.setAuthor(author);
        if (node.hasProperty(JcrDaoConstants.VRTX_COMMENT_TITLE)) {
            c.setTitle(node.getProperty(JcrDaoConstants.VRTX_COMMENT_TITLE).getString());
        }
        c.setContent(node.getProperty(JcrDaoConstants.VRTX_COMMENT_BODY).getString());
        c.setTime(node.getProperty(JcrDaoConstants.VRTX_COMMENT_TIME).getDate().getTime());
        Path uri = Path.fromString(JcrPathUtil.pathToUri(node.getParent().getParent().getPath()));
        c.setURI(uri);
        return c;
    }
    
    

    @Required public void setNodeTypesDefinition(org.springframework.core.io.Resource nodeTypesDefinition) {
        this.nodeTypesDefinition = nodeTypesDefinition;
    }
    
    
    @Required public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    private Session getSystemSession() {
        Credentials credentials = new SimpleCredentials("system", "".toCharArray());
        try {
            return repository.login(credentials);
        } catch (LoginException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        }
    }
    
    // XXX: Made public (JcrSqlSearcherImpl)
    public Session getSession() {
        //return getSystemSession();
        String name = "anonymous";
        try {
            name = SecurityContext.getSecurityContext().getPrincipal().getQualifiedName();
        } catch (Throwable t) {
            // Anonymous access...
        }

        Credentials credentials = new SimpleCredentials(name, "".toCharArray());
        try {
            return repository.login(credentials);
        } catch (LoginException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        }
    }

    public void destroy() throws Exception {
        if (this.systemSession != null) {
            systemSession.logout();
        }
    }

    public void afterPropertiesSet() throws Exception {
        
        this.systemSession = getSystemSession();
        
        try {
            Item item = systemSession.getItem(JcrDaoConstants.VRTX_ROOT);
            if (!item.isNode()) {
                throw new DataAccessException("Vortex root node '" + JcrDaoConstants.VRTX_ROOT + "' in jcr repo is property");
            }
        } catch (PathNotFoundException e) {
            logger.info("Initializing repository..");
            initializeResourceTypes();
            initializeRoot();
        }
    }
    
    private void initializeResourceTypes() throws RepositoryException, InvalidNodeTypeDefException, ParseException, IOException {
        InputStream stream = this.nodeTypesDefinition.getInputStream();
        Reader reader = new InputStreamReader(stream);
        String id = this.nodeTypesDefinition.getDescription();
        
        CompactNodeTypeDefReader cndReader = 
            new CompactNodeTypeDefReader(reader, id);

        NamespaceMapping nsMapping = cndReader.getNamespaceMapping();
        NamespaceRegistry nsRegistry = this.systemSession.getWorkspace().getNamespaceRegistry();
        nsRegistry.registerNamespace("vrtx", nsMapping.getURI("vrtx"));
        
        @SuppressWarnings("unchecked")
        List<NodeTypeDef> ntdList = cndReader.getNodeTypeDefs();
        NodeTypeManagerImpl ntmgr =(NodeTypeManagerImpl) this.systemSession.getWorkspace().getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        for (NodeTypeDef ntd: ntdList) {
            ntreg.registerNodeType(ntd);
        }    
    }

    
    private void initializeRoot() throws ValueFormatException,
        VersionException, LockException, ConstraintViolationException,
        RepositoryException, AccessDeniedException, ItemExistsException,
        InvalidItemStateException, NoSuchNodeTypeException {

        String now = Long.toString(System.currentTimeMillis());

        Node root = this.systemSession.getRootNode();
        root.addNode(JcrDaoConstants.LOCKS_ROOT.substring(1));

        root = root.addNode(JcrDaoConstants.VRTX_ROOT.substring(1), JcrDaoConstants.VRTX_FOLDER_NAME);
        root.setProperty(JcrDaoConstants.RESOURCE_TYPE, "collection");

        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.COLLECTION_PROP_NAME, "true");
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.OWNER_PROP_NAME, JcrDaoConstants.ROOT_USER);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.CREATIONTIME_PROP_NAME, now);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.CREATEDBY_PROP_NAME, JcrDaoConstants.ROOT_USER);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.LASTMODIFIED_PROP_NAME, now);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.MODIFIEDBY_PROP_NAME, JcrDaoConstants.ROOT_USER);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.CONTENTLASTMODIFIED_PROP_NAME, now);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.CONTENTMODIFIEDBY_PROP_NAME, JcrDaoConstants.ROOT_USER);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME, now);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME, JcrDaoConstants.ROOT_USER);
        root.setProperty(JcrDaoConstants.VRTX_PREFIX + PropertyType.CONTENTLENGTH_PROP_NAME, "0");
        
        setResourceNameProperty(root, Path.fromString("/"));

        Node aclNode = root.addNode(JcrDaoConstants.VRTX_ACL_NAME, JcrDaoConstants.VRTX_ACL_NAME);
        String pseudoType = Principal.Type.PSEUDO.name();
        Node action = aclNode.addNode(JcrDaoConstants.VRTX_PREFIX + "read", JcrDaoConstants.VRTX_ACTION_NAME).addNode(JcrDaoConstants.VRTX_PREFIX + JcrPathUtil.escapeIllegalJcrChars(PrincipalFactory.NAME_ALL), JcrDaoConstants.VRTX_PRINCIPAL_NAME);
        action.setProperty(JcrDaoConstants.VRTX_PRINCIPAL_TYPE_NAME, pseudoType);
        action = aclNode.addNode(JcrDaoConstants.VRTX_PREFIX + "all", JcrDaoConstants.VRTX_ACTION_NAME).addNode(JcrDaoConstants.VRTX_PREFIX + JcrPathUtil.escapeIllegalJcrChars(PrincipalFactory.NAME_OWNER), JcrDaoConstants.VRTX_PRINCIPAL_NAME);
        action.setProperty(JcrDaoConstants.VRTX_PRINCIPAL_TYPE_NAME, pseudoType);

        systemSession.save();
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
