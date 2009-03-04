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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Acl;
import org.vortikal.repository.AclImpl;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Privilege;
import org.vortikal.repository.Property;
import org.vortikal.repository.RepositoryAction;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

public final class NodeManager {

    private static final String NODE_DATA_RESOURCE_TYPE = "rtype";
    private static final String NODE_DATA_ACL = "acl";
    private static final String NODE_DATA_ACL_ACTION = "acl.action";
    private static final String NODE_DATA_ACL_USERS = "acl.users";
    private static final String NODE_DATA_ACL_GROUPS = "acl.groups";
    private static final String NODE_DATA_ACL_PSEUDOS = "acl.pseudos";
    private static final String NODE_DATA_PROP_NAMESPACE = "prop.namespace";
    private static final String NODE_DATA_PROP_VALUES = "prop.values";

    public static final String NODE_DATA_PROPS = "props";
    public static final String NODE_DATA_PROP_NAME = "prop.name";
    public static final String NODE_DATA_PROP_VALUE = "prop.value";

    private PrincipalFactory principalFactory;
    private ResourceTypeTree resourceTypeTree;
    private ResourceTypeEvaluator resourceTypeEvaluator;

    public final Node getRootNode() throws Exception {
        Principal rootPrincipal = this.principalFactory.getPrincipal("root@localhost",
                Principal.Type.USER);
        ResourceImpl rootResource = this.resourceTypeEvaluator.create(rootPrincipal, Path
                .fromString("/"), true);
        Acl acl = new AclImpl();
        acl.addEntry(Privilege.ALL, rootPrincipal);
        rootResource.setAcl(acl);
        return new Node(NodeID.rootID, null, new HashMap<String, NodeID>(),
                resourceToNodeData(rootResource));
    }

    public final JSONObject resourceToNodeData(ResourceImpl resource) throws Exception {
        JSONObject nodeData = new JSONObject();
        nodeData.put(NODE_DATA_RESOURCE_TYPE, resource.getResourceType());
        if (!resource.isInheritedAcl()) {
            JSONArray acl = createJSONAcl(resource.getAcl());
            nodeData.put(NODE_DATA_ACL, acl);
        }
        JSONArray props = new JSONArray();
        for (Property prop : resource.getProperties()) {
            JSONObject jsonProp = createJSONProp(prop);
            props.put(jsonProp);
        }
        nodeData.put(NODE_DATA_PROPS, props);
        return nodeData;
    }

    public final ResourceImpl nodeToResource(NodePath nodePath, Path uri) throws Exception {
        Node node = nodePath.getNode();

        JSONObject nodeData = node.getData();
        ResourceImpl r = new ResourceImpl(uri, this.resourceTypeTree);
        r.setResourceType(nodeData.getString(NODE_DATA_RESOURCE_TYPE));
        r.setNodeID(node.getNodeID());
        r.setUri(uri);

        JSONArray aclObj = null;
        Node aclNode = node;
        for (Iterator<Node> i = nodePath.towardsRoot(); i.hasNext();) {
            aclNode = i.next();
            if (aclNode.getData().has(NODE_DATA_ACL)) {
                aclObj = aclNode.getData().getJSONArray(NODE_DATA_ACL);
                break;
            }
        }
        if (aclObj == null) {
            throw new IllegalStateException("Must have at least one ACL resource (/)");
        }
        Acl acl = createAcl(aclObj);
        r.setAcl(acl);
        r.setInheritedAcl(false);
        if (aclNode != node) {
            r.setInheritedAcl(true);
            r.setAclInheritedFrom(aclNode.getNodeID());
        }

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

        PropertyTypeDefinition propDef = this.resourceTypeTree.getPropertyTypeDefinition(namespace,
                propName);

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
            throw new IllegalStateException("Invalid property object: missing value(s): "
                    + propsObject);
        }

        Property prop = propDef.createProperty(propValues);
        return prop;
    }

    private JSONObject createJSONProp(Property prop) throws Exception {
        JSONObject json = new JSONObject();
        json.put(NODE_DATA_PROP_NAMESPACE, prop.getDefinition().getNamespace().getUri());
        json.put(NODE_DATA_PROP_NAME, prop.getDefinition().getName());
        if (prop.getDefinition().isMultiple()) {
            Value[] values = prop.getValues();
            List<String> jsonValues = new ArrayList<String>(values.length);
            for (Value v : values) {
                jsonValues.add(v.getNativeStringRepresentation());
            }
            json.put(NODE_DATA_PROP_VALUES, jsonValues);
        } else {
            // XXX: Binary properties are created, stored and otherwise handled
            // by the repository. Only propIDs are set here
            if (prop.getDefinition().getType() == PropertyType.Type.BINARY) {
                PropertyID propID;
                if (prop.getValue().getNativeStringRepresentation() == null) {
                    String identifier = UUIDGenerator.getInstance().generateRandomBasedUUID()
                            .toString();
                    propID = PropertyID.valueOf(identifier);
                } else {
                    propID = PropertyID.valueOf(prop.getValue().getNativeStringRepresentation());
                }
                json.put(NODE_DATA_PROP_VALUE, propID.toString());
            } else {
                json.put(NODE_DATA_PROP_VALUE, prop.getValue().getNativeStringRepresentation());
            }
        }
        return json;
    }

    private Acl createAcl(JSONArray aclObject) throws Exception {
        Acl acl = new AclImpl();
        for (int i = 0; i < aclObject.length(); i++) {
            JSONObject ace = aclObject.getJSONObject(i);
            String action = ace.getString(NODE_DATA_ACL_ACTION);
            RepositoryAction priv = Privilege.getActionByName(action);

            if (ace.has(NODE_DATA_ACL_USERS)) {
                JSONArray users = ace.getJSONArray(NODE_DATA_ACL_USERS);
                for (int j = 0; j < users.length(); j++) {
                    String user = users.getString(j);
                    Principal p = this.principalFactory.getPrincipal(user, Principal.Type.USER);
                    acl.addEntry(priv, p);
                }
            }
            if (ace.has(NODE_DATA_ACL_GROUPS)) {
                JSONArray groups = ace.getJSONArray(NODE_DATA_ACL_GROUPS);
                for (int j = 0; j < groups.length(); j++) {
                    String group = groups.getString(j);
                    Principal p = this.principalFactory.getPrincipal(group, Principal.Type.GROUP);
                    acl.addEntry(priv, p);
                }
            }
            if (ace.has(NODE_DATA_ACL_PSEUDOS)) {
                JSONArray pseudos = ace.getJSONArray(NODE_DATA_ACL_PSEUDOS);
                for (int j = 0; j < pseudos.length(); j++) {
                    String pseudo = pseudos.getString(j);
                    Principal p = this.principalFactory.getPrincipal(pseudo, Principal.Type.PSEUDO);
                    acl.addEntry(priv, p);
                }
            }
        }
        return acl;
    }

    private JSONArray createJSONAcl(Acl acl) throws Exception {
        JSONArray arr = new JSONArray();
        Set<RepositoryAction> actions = acl.getActions();
        for (RepositoryAction action : actions) {
            JSONObject entry = new JSONObject();
            JSONArray users = new JSONArray();
            JSONArray groups = new JSONArray();
            JSONArray pseudos = new JSONArray();
            Set<Principal> principals = acl.getPrincipalSet(action);
            for (Principal principal : principals) {
                switch (principal.getType()) {
                case USER:
                    users.put(principal.getQualifiedName());
                    break;
                case GROUP:
                    groups.put(principal.getQualifiedName());
                    break;
                case PSEUDO:
                    pseudos.put(principal.getQualifiedName());
                    break;
                }
            }
            entry.put(NODE_DATA_ACL_ACTION, action.toString());
            if (users.length() > 0) {
                entry.put(NODE_DATA_ACL_USERS, users);
            }
            if (groups.length() > 0) {
                entry.put(NODE_DATA_ACL_GROUPS, groups);
            }
            if (pseudos.length() > 0) {
                entry.put(NODE_DATA_ACL_PSEUDOS, pseudos);
            }
            arr.put(entry);
        }
        return arr;
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

    @Required
    public void setResourceTypeTree(ResourceTypeTree resourceTypeTree) {
        this.resourceTypeTree = resourceTypeTree;
    }

    @Required
    public void setResourceTypeEvaluator(ResourceTypeEvaluator resourceTypeEvaluator) {
        this.resourceTypeEvaluator = resourceTypeEvaluator;
    }

}
