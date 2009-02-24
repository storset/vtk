/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repo2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AclImpl;
import org.vortikal.repository.AuthorizationException;
import org.vortikal.repository.Comment;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.IllegalOperationException;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.ResourceOverwriteException;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.AuthenticationException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.token.TokenManager;

public class Repo2 implements Repository, ApplicationContextAware {

    private static final String NODE_DATA_RESOURCE_TYPE = "rtype";
    private static final String NODE_DATA_PROPS = "props";
    private static final String NODE_DATA_PROP_NAMESPACE = "prop.namespace";
    private static final String NODE_DATA_PROP_NAME = "prop.name";
    private static final String NODE_DATA_PROP_VALUE = "prop.value";
    private static final String NODE_DATA_PROP_VALUES = "prop.values";
    
    private NodeStore store;
    private NodeID rootID = NodeID.valueOf("1000");

    // Stores:
    private ContentStore contentStore;
    private BinaryPropertyStore binaryPropertyStore;
    
    private ApplicationContext context;
    private TokenManager tokenManager;
    private ResourceTypeEvaluator resourceEvaluator;
    private AuthorizationManager authorizationManager;
    private File tempDir = new File(System.getProperty("java.io.tmpdir"));
    private String id;
    private int maxComments = 1000;
    private PeriodicThread periodicThread;
    private PrincipalFactory principalFactory;
    private ResourceTypeTree resourceTypeTree;
    
    public boolean isReadOnly() {
        return this.authorizationManager.isReadOnly();
    }


    public String getId() {
        return this.id;
    }
    
//    private List<Object> lock(List<Node> nodes) {
//        Collections.sort(nodes);
//        for (Node node: nodes) {
//            lock(node);
//        }
//        return null;
//    }
    
    private List<Node> load(Path uri) throws Exception {
        Node n = this.store.retrieve(this.rootID);
        List<Node> result = new ArrayList<Node>();
        result.add(n);
        List<String> elements = uri.getElements();
        for (int i = 1; i < uri.getElements().size(); i++) {
            String name = elements.get(i);
            NodeID childID = n.getChildID(name);
            if (childID == null) {
                return null;
            }
            n = this.store.retrieve(childID);
            result.add(n);
        }
        return result;
    }
    
    private void delete(Node n) {
        throw new IllegalStateException("delete() not implemented :(");
    }

    public boolean exists(String token, Path uri) throws Exception {
        List<Node> l = load(uri);
        return l != null;
    }
    
    private Property createProp(JSONObject propsObject) throws Exception {
        String propNamespace = null;
        if (propsObject.has(NODE_DATA_PROP_NAMESPACE)) {
            propNamespace = propsObject.getString(NODE_DATA_PROP_NAMESPACE);
        }
        String propName = propsObject.getString(NODE_DATA_PROP_NAME);

        Namespace namespace = Namespace.DEFAULT_NAMESPACE;
        if (propNamespace != null) {
            namespace = this.resourceTypeTree.getNamespace(propNamespace);
        }

        PropertyTypeDefinition propDef = 
            this.resourceTypeTree.getPropertyTypeDefinition(namespace, propName);
        
        String[] propValues = null;
        
        if (propsObject.has(NODE_DATA_PROP_VALUE)) {
            propValues = new String[1];
            propValues[0] = propsObject.getString(NODE_DATA_PROP_VALUE);
        } else if (propsObject.has(NODE_DATA_PROP_VALUES)) {
            JSONArray jsonArray = propsObject.getJSONArray(NODE_DATA_PROP_VALUES);
            propValues = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                propValues[i] = jsonArray.getString(i);
            }
        } else {
            throw new IllegalStateException("Invalid property object: missing value(s): " + propsObject);
        }
        
        Property prop = propDef.createProperty(propValues);
        return prop;
    }



    private void storeBinaryProps(ResourceImpl resource) throws Exception {
        
    }
    
    private JSONObject createJSONProp(Property prop) throws Exception {
        JSONObject json = new JSONObject();
        json.put(NODE_DATA_PROP_NAMESPACE, prop.getDefinition().getNamespace().getUri());
        json.put(NODE_DATA_PROP_NAME, prop.getDefinition().getName());
        if (prop.getDefinition().isMultiple()) {
            Value[] values = prop.getValues();
            List<String> jsonValues = new ArrayList<String>(values.length);
            for (Value v: values) {
                jsonValues.add(v.getNativeStringRepresentation());
            }
            json.put(NODE_DATA_PROP_VALUES, jsonValues);
        } else {
            if (prop.getDefinition().getType() == PropertyType.Type.BINARY) {
                PropertyID propID;
                if (prop.getValue().getNativeStringRepresentation() == null) {
                    String identifier = UUIDGenerator.getInstance().generateRandomBasedUUID().toString();
                    propID = PropertyID.valueOf(identifier);
                    this.binaryPropertyStore.create(propID);
                } else {
                    propID = PropertyID.valueOf(prop.getValue().getNativeStringRepresentation());
                }
                ContentStream cs = prop.getBinaryStream();
                TypedContentStream ts = new TypedContentStream(cs.getStream(), cs.getLength(), prop.getBinaryMimeType());
                this.binaryPropertyStore.update(propID, ts);
                json.put(NODE_DATA_PROP_VALUE, propID.toString());
            } else {
                json.put(NODE_DATA_PROP_VALUE, prop.getValue().getNativeStringRepresentation());
            }
        }
        return json;
    }

    private ResourceImpl nodeToResource(Node node, Path uri) throws Exception {
        JSONObject nodeData = node.getData();
        ResourceImpl r = new ResourceImpl(
                uri, this.resourceTypeTree);
        r.setResourceType(nodeData.getString(NODE_DATA_RESOURCE_TYPE));
        r.setID(-1);
        r.setNodeID(node.getNodeID().getIdentifier());
        r.setUri(uri);
        Acl acl = new AclImpl();
        r.setAcl(acl);
        JSONArray props = nodeData.getJSONArray(NODE_DATA_PROPS);
        for (int i = 0; i < props.length(); i++) {
            JSONObject propsObject = props.getJSONObject(i);
            r.addProperty(createProp(propsObject));
        }
        List<Path> childURIs = new ArrayList<Path>();
        for (String childName : node.getChildNames()) {
            Path childURI = uri.extend(childName);
            childURIs.add(childURI);
        }
        r.setChildURIs(childURIs.toArray(new Path[childURIs.size()]));
        return r;
    }
    
    private JSONObject resourceToNodeData(ResourceImpl resource) throws Exception {
        JSONObject nodeData = new JSONObject();
        nodeData.put(NODE_DATA_RESOURCE_TYPE, resource.getResourceType());
        JSONArray props = new JSONArray();
        for (Property prop: resource.getProperties()) {
            JSONObject jsonProp = createJSONProp(prop);
            props.put(jsonProp);
        }
        nodeData.put(NODE_DATA_PROPS, props);
        return nodeData;
    }
    

    public Resource retrieve(String token, Path uri, boolean forProcessing)
            throws ResourceNotFoundException, AuthorizationException, AuthenticationException,
            Exception {
        
        Principal principal = this.tokenManager.getPrincipal(token);
        List<Node> nodes = load(uri);
        if (nodes == null) 
            throw new ResourceNotFoundException(uri);
        Node node = nodes.get(nodes.size() - 1);
        if (forProcessing) {
            this.authorizationManager.authorizeReadProcessed(node, principal);
        } else {
            this.authorizationManager.authorizeRead(node, principal);
        }
        return nodeToResource(node, uri);
    }

    public InputStream getInputStream(String token, Path uri, boolean forProcessing)
            throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);
        List<Node> nodes = load(uri);
        if (nodes == null) 
            throw new ResourceNotFoundException(uri);
        Node node = nodes.get(nodes.size() - 1);
        if (forProcessing) {
            this.authorizationManager.authorizeReadProcessed(node, principal);
        } else {
            this.authorizationManager.authorizeRead(node, principal);
        }
        return this.contentStore.retrieve(node.getNodeID()).getStream();
    }


    public Resource[] listChildren(String token, Path uri, boolean forProcessing)
            throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);
        List<Node> nodes = load(uri);

        if (nodes == null) {
            throw new ResourceNotFoundException(uri);
        }
        
        Node node = nodes.get(nodes.size() - 1);
        Resource collection = nodeToResource(node, uri);

        if (!collection.isCollection()) {
            throw new IllegalOperationException("Can't list children for non-collection resources");
        }

        if (forProcessing) {
            this.authorizationManager.authorizeReadProcessed(node, principal);
        } else {
            this.authorizationManager.authorizeRead(node, principal);
        }
        
        List<NodeID> childIDs = new ArrayList<NodeID>(node.getChildIDs());
        List<Node> list = new ArrayList<Node>();
        if (childIDs.size() > 0) {
            list = this.store.retrieve(childIDs);
        }
//        ResourceImpl[] list = this.dao.loadChildren(collection);
        
        Resource[] children = new Resource[list.size()];
        int i = 0;
        for (Node n: list) {
            Path childURI = collection.getURI().extend(node.getChildName(n.getNodeID()));
            Resource child = nodeToResource(n, childURI);
            children[i++] = child;
        }
        return children;
    }


    public Resource createDocument(String token, Path uri) throws Exception {
        return create(token, uri, false);
    }


    public Resource createCollection(String token, Path uri) throws Exception {
        return create(token, uri, true);
    }


    public void copy(String token, Path srcUri, Path destUri, Repository.Depth depth,
            boolean overwrite, boolean preserveACL) throws Exception {
        
        throw new IllegalStateException("copy() not implemented :(");
    }


    public void move(String token, Path srcUri, Path destUri, boolean overwrite)
            throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);
        validateCopyURIs(srcUri, destUri);

        // Loading and checking source resource
        List<Node> srcNodes = load(srcUri);
        if (srcNodes == null) {
            throw new ResourceNotFoundException(srcUri);
        }
        Node srcNode = srcNodes.get(srcNodes.size() - 1);
        Node srcParentNode = srcNodes.get(srcNodes.size() - 2);
        ResourceImpl src = nodeToResource(srcNode, srcUri);
        
        // Checking dest
        List<Node> destNodes = load(destUri);
        if (destNodes != null && !overwrite) {
            throw new ResourceOverwriteException(destUri);
        }

        // checking destParent
        List<Node> destParentNodes = load(destUri.getParent());
        if (destParentNodes == null) {
            throw new IllegalOperationException("Invalid destination resource");
        }
        Node destParentNode = destParentNodes.get(destParentNodes.size() - 1);
        ResourceImpl destParent = nodeToResource(destParentNode, destUri.getParent());
        if (!destParent.isCollection()) {
            throw new IllegalOperationException("Invalid destination resource");
        }

        this.authorizationManager.authorizeMove(srcNode, destParentNode, principal, overwrite);

        // Performing delete operation
        if (destNodes != null) {
            ResourceImpl dest = nodeToResource(destNodes.get(destNodes.size() - 1), destUri);
            Node destNode = destNodes.get(destNodes.size() - 1);
            delete(destNode);
//            this.dao.delete(dest);
//            this.contentStore.deleteResource(dest.getURI());
//             this.context.publishEvent(new ResourceDeletionEvent(this, dest.getURI(), destNode.getNodeID(),
//                     dest.isCollection()));
        }

        try {
            srcParentNode.removeChild(srcUri.getName());
            if (!srcParentNode.getNodeID().equals(destParentNode.getNodeID())) {
                
            }
            destParentNode.addChild(destUri.getName(), srcNode.getNodeID());
            destParent = this.resourceEvaluator.contentModification(destParent, principal);

            JSONObject data = srcNode.getData();
            
            Node movedNode = new Node(srcNode.getNodeID(), destParentNode.getNodeID(), srcNode.getChildMap(), data);
            
            this.store.update(srcParentNode);
            this.store.update(destParentNode);
            this.store.update(movedNode);
            
//             this.context.publishEvent(new ResourceCreationEvent(this, nodeToResource(movedNode, destUri)));
//             this.context.publishEvent(new ResourceDeletionEvent(this, srcUri, movedNode.getNodeID(), src.isCollection()));

        } catch (CloneNotSupportedException e) {
            throw new IOException("clone() operation failed");
        }
    }


    public void delete(String token, Path uri) throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (uri.isRoot()) {
            throw new IllegalOperationException("Cannot delete the root resource ('/')");
        }

        List<Node> nodes = load(uri);
        if (nodes == null) {
            throw new ResourceNotFoundException(uri);
        }

        Node node = nodes.get(nodes.size() - 1);
        this.authorizationManager.authorizeDelete(node, principal);

        ResourceImpl r = nodeToResource(node, uri);

        Node parentNode = nodes.get(nodes.size() - 2);
        ResourceImpl parentCollection = nodeToResource(parentNode, uri);
        parentCollection = this.resourceEvaluator.contentModification(parentCollection, principal);

        parentNode = new Node(
                parentNode.getNodeID(), parentNode.getParentID(), 
                parentNode.getChildMap(), resourceToNodeData(parentCollection));
        parentNode.removeChild(uri.getName());
        this.store.update(parentNode);
        this.store.delete(node);

        this.contentStore.delete(node.getNodeID());
        
//         ResourceDeletionEvent event = 
//             new ResourceDeletionEvent(this, uri, node.getNodeID(), r.isCollection());
//         this.context.publishEvent(event);
    }


    public Resource lock(String token, Path uri, String ownerInfo, Repository.Depth depth,
            int requestedTimeoutSeconds, String lockToken) throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);
        if (depth == Depth.ONE || depth == Depth.INF) {
            throw new IllegalOperationException("Unsupported depth parameter: " + depth);
        }
        List<Node> nodes = load(uri);
        if (nodes == null) {
            throw new ResourceNotFoundException(uri);
        }
        Node n = nodes.get(nodes.size() - 1);
        ResourceImpl r = nodeToResource(n, uri);

        if (lockToken != null) {
            if (r.getLock() == null) {
                throw new IllegalOperationException("Invalid lock refresh request: lock token '"
                        + lockToken + "' does not exists on resource " + r.getURI());
            }
            if (!r.getLock().getLockToken().equals(lockToken)) {
                throw new IllegalOperationException("Invalid lock refresh request: lock token '"
                        + lockToken + "' does not match existing lock token on resource " + uri);
            }
        }

        this.authorizationManager.authorizeWrite(n, principal);
        
//        this.lockManager.lockResource(r, principal, ownerInfo, depth, requestedTimeoutSeconds,
//                (lockToken != null));

        return r;
    }


    public void unlock(String token, Path uri, String lockToken) throws Exception {
//        Principal principal = this.tokenManager.getPrincipal(token);
//        ResourceImpl r = this.dao.load(uri);
//        if (r == null) {
//            throw new ResourceNotFoundException(uri);
//        }
//
//        this.authorizationManager.authorizeUnlock(uri, principal);
//
//        if (r.getLock() != null) {
//            r.setLock(null);
//            this.dao.store(r);
//        }
    }


    public Resource store(String token, Resource resource) throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);

        if (resource == null) {
            throw new IllegalOperationException("Can't store nothingness.");
        }

        if (!(resource instanceof ResourceImpl)) {
            throw new IllegalOperationException("Can't store unknown implementation of resource..");
        }
        Path uri = resource.getURI();

        List<Node> nodes = load(uri);
        if (nodes == null) {
            throw new ResourceNotFoundException(uri);
        }
        Node n = nodes.get(nodes.size() - 1);
        ResourceImpl original = nodeToResource(n, uri);
        if (original == null) {
            throw new ResourceNotFoundException(uri);
        }

        this.authorizationManager.authorizeWrite(n, principal);
        ResourceImpl originalClone = (ResourceImpl) original.clone();

        ResourceImpl newResource = this.resourceEvaluator.propertiesChange(original, principal,
                (ResourceImpl) resource);
        n = new Node(n.getNodeID(), n.getParentID(), n.getChildMap(), resourceToNodeData(newResource));
        this.store.update(n);
//         ResourceModificationEvent event = new ResourceModificationEvent(this, newResource,
//                 originalClone);
//         this.context.publishEvent(event);
        return newResource;
    }


    /**
     * Requests that an InputStream be written to a resource.
     */
    public Resource storeContent(String token, Path uri, InputStream byteStream)
            throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);

        List<Node> nodes = load(uri);
        if (nodes == null) {
            throw new ResourceNotFoundException(uri);
        }
        Node n = nodes.get(nodes.size() - 1);
        ResourceImpl r = nodeToResource(n, uri);

        if (r.isCollection()) {
            throw new IllegalOperationException("resource is collection");
        }

        this.authorizationManager.authorizeWrite(n, principal);
        File tempFile = null;
        try {
            // Write to a temporary file to avoid locking:
            tempFile = writeTempFile(r.getName(), byteStream);
            Resource original = (ResourceImpl) r.clone();
            ContentStream content = new ContentStream(new java.io.BufferedInputStream(
                    new java.io.FileInputStream(tempFile)), tempFile.length());
            this.contentStore.update(n.getNodeID(), content);
            r = this.resourceEvaluator.contentModification(r, principal);
            n = new Node(n.getNodeID(), n.getParentID(), n.getChildMap(), resourceToNodeData(r));
            this.store.update(n);

//             ContentModificationEvent event = new ContentModificationEvent(this, (Resource) r
//                     .clone(), original);
//             this.context.publishEvent(event);
            return r;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }


    public void storeACL(String token, Resource resource) throws Exception {

        if (resource == null) {
            throw new IllegalArgumentException("Resource is null");
        }

        Principal principal = this.tokenManager.getPrincipal(token);
        List<Node> nodes = load(resource.getURI());
        if (nodes == null) {
            throw new ResourceNotFoundException(resource.getURI());
        }
        Node n = nodes.get(nodes.size() - 1);
        ResourceImpl r = nodeToResource(n, resource.getURI());
        this.authorizationManager.authorizeAll(n, principal);

        try {
            Resource original = (Resource) r.clone();

            if (original.isInheritedAcl() && resource.isInheritedAcl()) {
                /* No ACL change */
                return;
            }
            ResourceImpl parent = null;
            if (!r.getURI().isRoot()) {
                Node parentNode = nodes.get(nodes.size() - 2);
                parent = nodeToResource(parentNode, r.getURI().getParent());
            }

            if (resource.getURI().isRoot() && resource.isInheritedAcl()) {
                throw new IllegalOperationException(
                        "The root resource cannot have an inherited ACL");
            }

            if (original.isInheritedAcl() && !resource.isInheritedAcl()) {
                /*
                 * Switching from inheritance. Make the new ACL a copy of the
                 * parent's ACL, since the supplied one may contain other ACEs
                 * than the one we now inherit from.
                 */
                AclImpl newAcl = (AclImpl) parent.getAcl().clone();
                r.setAcl(newAcl);
                r.setInheritedAcl(false);
                r.setAclInheritedFrom(PropertySetImpl.NULL_RESOURCE_ID);

            } else if (!original.isInheritedAcl() && resource.isInheritedAcl()) {
                /* Switching to inheritance. */
                r.setAclInheritedFrom(parent.getID());
                r.setInheritedAcl(true);

            } else {
                /* Updating the entries */
                AclImpl newAcl = (AclImpl) resource.getAcl().clone();
                r.setInheritedAcl(false);
                r.setAcl(newAcl);
            }

            n = new Node(n.getNodeID(), n.getParentID(), n.getChildMap(), resourceToNodeData(r));
            this.store.update(n);

//             ACLModificationEvent event = new ACLModificationEvent(this, (Resource) r.clone(),
//                     original, r.getAcl(), original.getAcl());

//             this.context.publishEvent(event);

        } catch (CloneNotSupportedException e) {
            throw new IOException(e.getMessage());
        }
    }


    public List<Comment> getComments(String token, Resource resource) {
        return getComments(token, resource, false, 500);
    }


    public List<Comment> getComments(String token, Resource resource, boolean deep, int max) {
        return null;
//        Principal principal = this.tokenManager.getPrincipal(token);
//
//        if (resource == null) {
//            throw new IllegalOperationException("Resource argument cannot be NULL");
//        }
//
//        try {
//            ResourceImpl original = this.dao.load(resource.getURI());
//            if (original == null) {
//                throw new ResourceNotFoundException(resource.getURI());
//            }
//            this.authorizationManager.authorizeReadProcessed(resource.getURI(), principal);
//            List<Comment> comments = this.commentDAO.listCommentsByResource(resource, deep, max);
//            List<Comment> result = new ArrayList<Comment>();
//            Set<Path> authCache = new HashSet<Path>();
//            // Fetch N comments, authorize on the result set:
//            for (Comment c : comments) {
//                try {
//                    if (!authCache.contains(c.getURI())) {
//                        this.authorizationManager.authorizeReadProcessed(c.getURI(), principal);
//                        authCache.add(c.getURI());
//                    }
//                    result.add(c);
//                } catch (Throwable t) {
//                }
//            }
//            return Collections.unmodifiableList(result);
//        } catch (IOException e) {
//            throw new RuntimeException("Unhandled IO exception", e);
//        }
    }


    public Comment addComment(String token, Resource resource, String title, String text) {
        throw new IllegalStateException("addComment() not implemented :(");
    }


    public void deleteComment(String token, Resource resource, Comment comment) {
        throw new IllegalStateException("deleteComment() not implemented :(");
    }


    public void deleteAllComments(String token, Resource resource) {
        throw new IllegalStateException("deleteAllComments() not implemented :(");
    }


    public Comment updateComment(String token, Resource resource, Comment comment) {
        throw new IllegalStateException("updateAllComments() not implemented :(");
    }

    private Resource create(String token, Path uri, boolean collection)
            throws Exception {

        Principal principal = this.tokenManager.getPrincipal(token);
        List<Node> nodes = load(uri.getParent());
        if (nodes == null) {
            throw new IllegalOperationException("Cannot create resource " 
                    + uri + ": parent does not exist");
        }
        Node parentNode = nodes.get(nodes.size() - 1);
        if (parentNode.getChildNames().contains(uri.getName())) {
            throw new ResourceOverwriteException(uri);
        }
        
        ResourceImpl parent = nodeToResource(parentNode, uri.getParent());
        if (!parent.isCollection()) {
            throw new IllegalOperationException("Cannot create resource " 
                    + uri + ": parent is not a collection");
        }

        this.authorizationManager.authorizeCreate(parentNode, principal);

        ResourceImpl newResource = this.resourceEvaluator.create(principal, uri, collection);

        try {
//            Acl newAcl = (Acl) parent.getAcl().clone();
//            newResource.setAcl(newAcl);
//            newResource.setInheritedAcl(true);
//            int aclIneritedFrom = parent.isInheritedAcl() ? parent.getAclInheritedFrom() : parent
//                    .getID();
//            newResource.setAclInheritedFrom(aclIneritedFrom);
            JSONObject nodeData = resourceToNodeData(newResource);
            String identifier = UUIDGenerator.getInstance().generateRandomBasedUUID().toString();
            NodeID id = NodeID.valueOf(identifier);
            Node n = new Node(id, parentNode.getNodeID(), new HashMap<String, NodeID>(), nodeData);
            this.store.create(n);
            parent = this.resourceEvaluator.contentModification(parent, principal);
            parentNode = new Node(parentNode.getNodeID(), parentNode.getParentID(), parentNode.getChildMap(), resourceToNodeData(parent));
            parentNode.addChild(uri.getName(), n.getNodeID());
            this.store.update(parentNode);
            if (!collection)  {
                this.contentStore.create(id);
            }
            //this.dao.store(newResource);
            //this.contentStore.createResource(newResource.getURI(), collection);

            //newResource = this.dao.load(uri);

//            parent.addChildURI(uri);
//            parent = this.resourceEvaluator.contentModification(parent, principal);
//
//            this.dao.store(parent);
//
//            newResource = (ResourceImpl) newResource.clone();
        } catch (CloneNotSupportedException e) {
            throw new IOException("An internal error occurred: unable to " + "clone() resource: "
                    + uri);
        }

//         this.context.publishEvent(new ResourceCreationEvent(this, newResource));
        return newResource;
    }


    /**
     * Writes to a temporary file (used to avoid lengthy blocking on file
     * uploads).
     * 
     * XXX: should be handled on the client side?
     */
    private File writeTempFile(String name, InputStream byteStream) throws IOException {
        ReadableByteChannel src = Channels.newChannel(byteStream);
        File tempFile = File.createTempFile("tmpfile-" + name, null, this.tempDir);
        FileChannel dest = new FileOutputStream(tempFile).getChannel();
        int chunk = 100000;
        long pos = 0;
        while (true) {
            long n = dest.transferFrom(src, pos, chunk);
            if (n == 0) {
                break;
            }
            pos += n;
        }
        src.close();
        dest.close();
        return tempFile;
    }


    public void setReadOnly(String token, boolean readOnly) throws AuthorizationException {

        Principal principal = this.tokenManager.getPrincipal(token);
        this.authorizationManager.authorizeRootRoleAction(principal);
        this.authorizationManager.setReadOnly(readOnly);
    }


    private void periodicJob() {
//        if (!this.isReadOnly()) {
//            this.dao.deleteExpiredLocks(new Date());
//        }
    }


    @Required
    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


//    @Required
//    public void setDao(DataAccessor dao) {
//        this.dao = dao;
//    }


//    @Required
//    public void setCommentDAO(CommentDAO commentDAO) {
//        this.commentDAO = commentDAO;
//    }


    @Required
    public void setId(String id) {
        this.id = id;
    }


//    @Required
//    public void setLockManager(LockManager lockManager) {
//        this.lockManager = lockManager;
//    }


    @Required
    public void setAuthorizationManager(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }


    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }


    @Required
    public void setResourceTypeEvaluator(ResourceTypeEvaluator resourceEvaluator) {
        this.resourceEvaluator = resourceEvaluator;
    }


    public void setTempDir(String tempDirPath) {
        File tmp = new File(tempDirPath);
        if (!tmp.exists()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " + tmp
                    + " does not exist");
        }
        if (!tmp.isDirectory()) {
            throw new IllegalArgumentException("Unable to set tempDir: file " + tmp
                    + " is not a directory");
        }
        this.tempDir = tmp;
    }


    public void setMaxComments(int maxComments) {
        if (maxComments < 0) {
            throw new IllegalArgumentException("Argument must be an integer >= 0");
        }

        this.maxComments = maxComments;
    }


    @Required public void setStore(NodeStore store) {
        this.store = store;
    }


    @Required public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }
    
    @Required public void setContentStore(ContentStore store) {
        this.contentStore = store;
    }

    @Required public void setBinaryPropertyStore(BinaryPropertyStore store) {
        this.binaryPropertyStore = store;
    }

    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }


    public void init() throws Exception {
        this.periodicThread = new PeriodicThread(600);
        this.periodicThread.start();
    }

    
    public void initRootNode() throws Exception {
        Node root = null;
        try {
            root = this.store.retrieve(this.rootID);
        } catch (Throwable t) { }
        if (root == null) {
            Principal rootPrincipal = this.principalFactory.getPrincipal("root@localhost", Principal.Type.USER);
            ResourceImpl rootResource = this.resourceEvaluator.create(rootPrincipal, Path.fromString("/"), true);
            Node rootNode = new Node(this.rootID, null, new HashMap<String, NodeID>(), resourceToNodeData(rootResource));
            this.store.create(rootNode);
        }
    }

    public void destroy() {
        this.periodicThread.kill();
    }

    private static Log periodicLogger = LogFactory.getLog(PeriodicThread.class);

    private class PeriodicThread extends Thread {

        private long sleepSeconds;
        private boolean alive = true;


        public PeriodicThread(long sleepSeconds) {
            this.sleepSeconds = sleepSeconds;
        }


        public void kill() {
            this.alive = false;
            this.interrupt();
        }


        public void run() {
            while (this.alive) {
                try {
                    sleep(1000 * this.sleepSeconds);
                    periodicJob();
                } catch (InterruptedException e) {
                    this.alive = false;
                } catch (Throwable t) {
                    periodicLogger.warn("Caught exception in cleanup thread", t);
                }
            }
            periodicLogger.info("Terminating refresh thread");
        }
    }


    private void validateCopyURIs(Path srcPath, Path destPath)
            throws IllegalOperationException {

        if (srcPath.isRoot()) {
            throw new IllegalOperationException(
                    "Cannot copy or move the root resource ('/')");
        }

        if (destPath.isRoot()) {
            throw new IllegalOperationException(
                    "Cannot copy or move to the root resource ('/')");
        }

        if (destPath.equals(srcPath)) {
            throw new IllegalOperationException(
                    "Cannot copy or move a resource to itself");
        }

        if (srcPath.isAncestorOf(destPath)) {
            throw new IllegalOperationException(
                    "Cannot copy or move a resource into itself");
        }
    }

}
