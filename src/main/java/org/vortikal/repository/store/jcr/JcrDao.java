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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
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
import javax.jcr.version.VersionException;

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
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.AclImpl;
import org.vortikal.repository.ChangeLogEntry;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.store.ContentStore;
import org.vortikal.repository.store.DataAccessException;
import org.vortikal.repository.store.DataAccessor;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;

public class JcrDao implements ContentStore, DataAccessor, DisposableBean {

    private static final Log logger = LogFactory.getLog(JcrDao.class);

    private static final String VRTX_PREFIX = "vrtx:";
    private static final String VRTX_FILE_NAME = "vrtx:file";
    private static final String VRTX_FOLDER_NAME = "vrtx:folder";

    private static final String RESOURCE_TYPES_FILE = "resource-types.cnd";


    private static final String VRTX_ROOT = "/vrtx";

    private static final String CONTENT = "vrtx:content";
    private static final String RESOURCE_TYPE = "vrtx:resourceType";

    Repository repository;
    Session keepAliveSession;

    private String defaultUser = "root@localhost";

    public void destroy() throws Exception {
        if (keepAliveSession != null) {
            keepAliveSession.logout();
        }

    }

    Session getSession() {
        Session session;
        try {
            String name = "system";
            try {
                Principal principal = SecurityContext.getSecurityContext().getPrincipal();
                if (principal != null) { 
                    name = principal.getName();
                }
            } catch (Throwable t) {
                // System access...
            }

            Credentials credentials = new SimpleCredentials(name, "".toCharArray());
            session = repository.login(credentials);
        } catch (LoginException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        }
        return session;
    }

    public void copy(ResourceImpl resource, ResourceImpl dest,
            PropertySet newResource, boolean copyACLs,
            PropertySet fixedProperties) throws DataAccessException {

        Session session = getSession();

        String path = uriToPath(resource.getURI());
        String newPath = uriToPath(newResource.getURI());

        try {
            Workspace workspace = session.getWorkspace();
            workspace.copy(path, newPath);
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }

    }

    private String pathToUri(String path) {
        if (VRTX_ROOT.equals(path)) {
            return "/";
        }
        return unescapeIllegalJcrChars(path.substring(VRTX_ROOT.length()));
    }
    private String uriToPath(String uri) {
        if ("/".equals(uri)) {
            return VRTX_ROOT;
        }
        return VRTX_ROOT + escapeIllegalJcrChars(uri);
         
    }

    public void delete(ResourceImpl resource) throws DataAccessException {

        Session session = getSession();

        String path = uriToPath(resource.getURI());

        try {
            Node node = (Node)session.getItem(path);
            aquireLockToken(session, node);
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

    private void aquireLockToken(Session session, Node node)
            throws RepositoryException {
        if (node.isLocked()) {
            synchronized (this.keepAliveSession) {
                Node lockNode = session.getRootNode().getNode("locks");  
                lockNode = lockNode.getNode(node.getLock().getNode().getUUID());
                if (lockNode.getProperty("owner").getString().equals(session.getUserID()))
                    session.addLockToken(lockNode.getProperty("jcrLockToken").getString());
            } 
        }
    }

    public ResourceImpl load(String uri) throws DataAccessException {
        Session session = getSession();

        String path = uriToPath(uri);

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

    private String[] getChildUris(Node node) throws RepositoryException {
        List<String> children = new ArrayList<String>();

        for (NodeIterator i = node.getNodes(); i.hasNext();) {
            String uri = pathToUri(i.nextNode().getPath());
            children.add(uri);
        }
        return children.toArray(new String[children.size()]);
    }

    private ResourceImpl nodeToResource(Node node) throws RepositoryException {
        String uri = pathToUri(node.getPath());
        
        ResourceImpl resource = createResourceImpl();

        resource.setUri(uri);
        logger.debug("Node to resource: '" + uri + "'");
        String resourceType = node.getProperty(RESOURCE_TYPE).getString();
        logger.debug("Prop: " + RESOURCE_TYPE + " value: " + resourceType);

        resource.setResourceType(resourceType);

        for (PropertyIterator properties = node.getProperties(VRTX_PREFIX + "*"); properties.hasNext();) {
            Property prop = properties.nextProperty();

            String name = prop.getName();
            if (name.equals(RESOURCE_TYPE) || name.equals(CONTENT)) {
                continue;
            }

            name = name.substring(VRTX_PREFIX.length());

            String prefix = null;
            int sepIndex = name.indexOf(':');
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
            logger.debug("Prop: " + name + " values: " + stringValues);

            resource.createProperty(prefix, name, stringValues);
        }
        if (node.isLocked()) {
            Lock lock = node.getLock();
            resource.setLock(getLock(lock, node.getSession()));
        }

        if (resource.isCollection()) {
            resource.setChildURIs(getChildUris(node));
        }

        resource.setAcl(new AclImpl());

        return resource;

    }

    private org.vortikal.repository.Lock getLock(Lock lock, Session session) throws PathNotFoundException, RepositoryException {
        Node lockNode = session.getRootNode().getNode("locks");  
        String uuid = lock.getNode().getUUID();
        Node node = lockNode.getNode(uuid);
        String token = node.getProperty("lockToken").getString();
        String ownerInfo = node.getProperty("ownerInfo").getString();
        String owner = node.getProperty("owner").getString();
        String depth = node.getProperty("depth").getString();
        Date timeOut = new Date(Long.parseLong(node.getProperty("timeOut").getString()));
        
        logger.debug("Building lock for '" + uuid + "': " + node.getProperty("owner").getString() + ", " + token);
        org.vortikal.repository.LockImpl vLock = new org.vortikal.repository.LockImpl(
                token, new Principal(owner, Principal.Type.USER), ownerInfo, depth, timeOut);

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

        String parentPath = uriToPath(parent.getURI());

        try {
            List<ResourceImpl> children = new ArrayList<ResourceImpl>();

            Node node = (Node) session.getItem(parentPath);

            for (NodeIterator i = node.getNodes(); i.hasNext();) {
                Node child = i.nextNode();
                logger.debug("Child '" + child.getPath() + "'");
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

        String path = uriToPath(resource.getURI());
        String newPath = uriToPath(newResource.getURI());


        try {
            session.getWorkspace().move(path, newPath);
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

        String parentPath = uriToPath(resource.getParent());

        try {
            Node node = (Node) session.getItem(parentPath);
            aquireLockToken(session, node);

            String primaryResourceType = (resource.isCollection()) ? VRTX_FOLDER_NAME : VRTX_FILE_NAME;
            node = node.addNode(escapeIllegalJcrChars(resource.getName()), primaryResourceType);

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

            node.setProperty(RESOURCE_TYPE, resource.getResourceType());

            if (!resource.isCollection()) {
                node.setProperty(CONTENT, new ByteArrayInputStream(new byte[0]));
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
            propName = prefix + ":" + propName;
        }
        propName = VRTX_PREFIX + propName;
        return propName;
    }

    public String[] discoverLocks(String uri) throws DataAccessException {
        Session session = getSession();

        String path = uriToPath(uri);

        try {
            Node node = (Node) session.getItem(path);
            if (!node.isLocked()) {
                return new String[0];
            }
            
            String lockUri = pathToUri(node.getLock().getNode().getPath());
            return new String[] { lockUri };
        } catch (PathNotFoundException e) {
            return new String[0];
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }

    }

    public void store(ResourceImpl r) throws DataAccessException {
        Session session = getSession();

        String path = uriToPath(r.getURI());

        try {
            Node node = null;
            try {
                node = (Node) session.getItem(path);                
            } catch (PathNotFoundException e) {
                create(r);
                return;
            }
            aquireLockToken(session, node);

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

            for (PropertyIterator i = node.getProperties(VRTX_PREFIX + "*"); i.hasNext();) {
                Property p = i.nextProperty();

                String name = p.getName();
                if (!name.equals(CONTENT) && !vProps.contains(name)) {
                    toRemove.add(p);
                }
            }

            for (Property property : toRemove) {
                if (!property.getDefinition().isProtected()) {
                    property.remove();
                }
            }

            node.setProperty(RESOURCE_TYPE, r.getResourceType());

            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    private void unlock(Session session, Node node) throws RepositoryException {
        node.unlock();
        synchronized (this.keepAliveSession) {
            Node lockNode = session.getRootNode().getNode("locks");  
            Node node2 = lockNode.getNode(node.getUUID());
            logger.debug("Unlocking '" + node.getPath() + "': " + node2.getProperty("owner").getString());
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

        logger.debug("Locking '" + node.getPath() + "', '"+ node.getUUID()+ "': " + node.getLock().getLockToken() + ", " + session.getUserID());

        synchronized (this.keepAliveSession) {
          Node lockNode = session.getRootNode().getNode("locks");  
          lockNode = lockNode.addNode(node.getUUID());
          lockNode.setProperty("jcrLockToken", node.getLock().getLockToken());
          lockNode.setProperty("lockToken", lock.getLockToken());
          lockNode.setProperty("depth", lock.getDepth());
          lockNode.setProperty("ownerInfo", lock.getOwnerInfo());
          lockNode.setProperty("owner", lock.getPrincipal().getQualifiedName());
          lockNode.setProperty("timeOut", lock.getTimeout().getTime());
          session.save();
        }
    }

    public void storeContent(String uri, InputStream byteStream) {
        Session session = getSession();

        String path = uriToPath(uri);
        
        try {
            Node node = (Node) session.getItem(path);
            aquireLockToken(session, node);
            node.setProperty(CONTENT, byteStream);
            session.save();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void storeACL(ResourceImpl r) throws DataAccessException {
        // XXX Auto-generated method stub

    }

    public boolean validate() throws DataAccessException {
        return true;
    }

    public void addChangeLogEntry(ChangeLogEntry entry, boolean recurse)
            throws DataAccessException {
        logger.info("## Nicely formatted change log entry for "
                + entry.getOperation().name() + " on " + entry.getUri());

    }

    public void deleteExpiredLocks() throws DataAccessException {
        // XXX Auto-generated method stub
    }

    public String[] discoverACLs(String uri) throws DataAccessException {
        return new String[] { uri };
    }

    public Set<Principal> discoverGroups() throws DataAccessException {
        return new HashSet<Principal>();
    }

    public String[] listSubTree(ResourceImpl parent) throws DataAccessException {
        // Never called
        return null;
    }

    // Content store:

    public void copy(String srcURI, String destURI) throws DataAccessException {
        // Never called
    }

    public void createResource(String uri, boolean isCollection)
            throws DataAccessException {
        // Never called
    }

    public void deleteResource(String uri) throws DataAccessException {
        // Never called
    }

    public long getContentLength(String uri) throws DataAccessException {
        Session session = getSession();

        String path = uriToPath(uri);
        
        try {
            Node node = (Node) session.getItem(path);
            return node.getProperty(CONTENT).getLength();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public InputStream getInputStream(String uri) throws DataAccessException {
        Session session = getSession();

        String path = uriToPath(uri);

        try {
            Node node = (Node) session.getItem(path);
            return node.getProperty(CONTENT).getStream();
        } catch (PathNotFoundException e) {
            throw new DataAccessException(e);
        } catch (RepositoryException e) {
            throw new DataAccessException(e);
        } finally {
            session.logout();
        }
    }

    public void move(String srcURI, String destURI) throws DataAccessException {
        // Never called
    }

    @Required public void setRepository(Repository repository) 
        throws RepositoryException, InvalidNodeTypeDefException, ParseException {
        this.repository = repository;
        this.keepAliveSession = getSession();

            try {
                Item item = keepAliveSession.getItem(VRTX_ROOT);
                if (!item.isNode()) {
                    throw new DataAccessException("Vortex root node '" + VRTX_ROOT + "' in jcr repo is property");
                }
            } catch (PathNotFoundException e) {
                logger.info("Initializing repository..");
                initializeResourceTypes();
                initializeRoot();
            }
    }

    private void initializeResourceTypes() throws RepositoryException, InvalidNodeTypeDefException, ParseException {
        InputStream stream = this.getClass().getResourceAsStream(RESOURCE_TYPES_FILE);

        
        
        Reader reader = new InputStreamReader(stream);
        
        CompactNodeTypeDefReader cndReader = 
            new CompactNodeTypeDefReader(reader, RESOURCE_TYPES_FILE);

        NamespaceMapping nsMapping = cndReader.getNamespaceMapping();
        NamespaceRegistry nsRegistry = this.keepAliveSession.getWorkspace().getNamespaceRegistry();
        nsRegistry.registerNamespace("vrtx", nsMapping.getURI("vrtx"));
        
        List<NodeTypeDef> ntdList = cndReader.getNodeTypeDefs();
        NodeTypeManagerImpl ntmgr =(NodeTypeManagerImpl) this.keepAliveSession.getWorkspace().getNodeTypeManager();
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

        Node root = this.keepAliveSession.getRootNode();
        root.addNode("locks");
        
        root = root.addNode(VRTX_ROOT.substring(1), VRTX_FOLDER_NAME);
        root.setProperty(RESOURCE_TYPE, "collection");

        root.setProperty(VRTX_PREFIX + PropertyType.COLLECTION_PROP_NAME, "true");
        root.setProperty(VRTX_PREFIX + PropertyType.OWNER_PROP_NAME, defaultUser);
        root.setProperty(VRTX_PREFIX + PropertyType.CREATIONTIME_PROP_NAME, now);
        root.setProperty(VRTX_PREFIX + PropertyType.CREATEDBY_PROP_NAME, defaultUser);
        root.setProperty(VRTX_PREFIX + PropertyType.LASTMODIFIED_PROP_NAME, now);
        root.setProperty(VRTX_PREFIX + PropertyType.MODIFIEDBY_PROP_NAME, defaultUser);
        root.setProperty(VRTX_PREFIX + PropertyType.CONTENTLASTMODIFIED_PROP_NAME, now);
        root.setProperty(VRTX_PREFIX + PropertyType.CONTENTMODIFIEDBY_PROP_NAME, defaultUser);
        root.setProperty(VRTX_PREFIX + PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME, now);
        root.setProperty(VRTX_PREFIX + PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME, defaultUser);
        root.setProperty(VRTX_PREFIX + PropertyType.CONTENTLENGTH_PROP_NAME, "0");
        keepAliveSession.save();
    }

    
    
    
    /**
     * Escapes all illegal JCR name characters of a string.
     * The encoding is loosely modeled after URI encoding, but only encodes
     * the characters it absolutely needs to in order to make the resulting
     * string a valid JCR name.
     * Use {@link #unescapeIllegalJcrChars(String)} for decoding.
     * <p/>
     * QName EBNF:<br>
     * <xmp>
     * simplename ::= onecharsimplename | twocharsimplename | threeormorecharname
     * onecharsimplename ::= (* Any Unicode character except: '.', '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * twocharsimplename ::= '.' onecharsimplename | onecharsimplename '.' | onecharsimplename onecharsimplename
     * threeormorecharname ::= nonspace string nonspace
     * string ::= char | string char
     * char ::= nonspace | ' '
     * nonspace ::= (* Any Unicode character except: '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * </xmp>
     *
     * @param name the name to escape
     * @return the escaped name
     */
    public static String escapeIllegalJcrChars(String path) {
        StringBuffer buffer = new StringBuffer(path.length() * 2);
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '%' || ch == ':' || ch == '[' || ch == ']'
                || ch == '*' || ch == '\'' || ch == '"' || ch == '|'
                || (ch == '.' && path.length() < 3)
                || (ch == ' ' && (i == 0 || i == path.length() - 1))
                || ch == '\t' || ch == '\r' || ch == '\n') {
                buffer.append('%');
                buffer.append(Character.toUpperCase(Character.forDigit(ch / 16, 16)));
                buffer.append(Character.toUpperCase(Character.forDigit(ch % 16, 16)));
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /**
     * Unescapes previously escaped jcr chars.
     * <p/>
     * Please note, that this does not exactly the same as the url related
     * {@link #unescape(String)}, since it handles the byte-encoding
     * differently.
     *
     * @param name the name to unescape
     * @return the unescaped name
     */
    public static String unescapeIllegalJcrChars(String path) {
        StringBuffer buffer = new StringBuffer(path.length());
        int i = path.indexOf('%');
        while (i > -1 && i + 2 < path.length()) {
            buffer.append(path.toCharArray(), 0, i);
            int a = Character.digit(path.charAt(i + 1), 16);
            int b = Character.digit(path.charAt(i + 2), 16);
            if (a > -1 && b > -1) {
                buffer.append((char) (a * 16 + b));
                path = path.substring(i + 3);
            } else {
                buffer.append('%');
                path = path.substring(i + 1);
            }
            i = path.indexOf('%');
        }
        buffer.append(path);
        return buffer.toString();
    }

    
}
