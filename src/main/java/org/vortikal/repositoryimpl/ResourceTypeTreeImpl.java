/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.ResourceTypeTree;
import org.vortikal.repository.resourcetype.MixinResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PrimaryResourceTypeDefinition;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.ResourceTypeDefinition;

public class ResourceTypeTreeImpl implements InitializingBean, ApplicationContextAware, ResourceTypeTree {

    private static Log logger = LogFactory.getLog(ResourceTypeTreeImpl.class);
    
    private ApplicationContext applicationContext;
    
    
    private PrimaryResourceTypeDefinition rootResourceTypeDefinition;
    
    /* Currently maps a parent resource type def. to its children (arrays)
     * XXX: Resource type definitions that have no children are _not_
     *      represented as keys in this map. Beware if using key set iteration.
     */
    private Map resourceTypeDefinitions = new HashMap();
    
    /**
     * Maps resource type names to resource type objects
     */
    private Map resourceTypeNameMap = new HashMap();

    // Currently maps namespaceUris to maps which map property names to defs.
    private Map propertyTypeDefinitions = new HashMap();
    
    private Collection mixins;

    private Map mixinTypeDefinitionMap = new HashMap();

    private Map namespaceUriMap = new HashMap();
    
    private Map namespacePrefixMap = new HashMap();

    /* Map resource type name to flat list of _all_ descendant resource type names.
     * (Supports fast lookup for 'IN'-resource-type queries)
     */
    private Map resourceTypeDescendantNames;

    private void addNamespacesAndProperties(ResourceTypeDefinition def) {
        // Populate map of property type definitions
        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        Namespace namespace = def.getNamespace();

        if (!this.namespaceUriMap.containsKey(def.getNamespace().getUri())) {
            logger.info("Adding namespace URI mapping: " + def.getNamespace().getUri()
                        + " -> " + def.getNamespace());
            this.namespaceUriMap.put(def.getNamespace().getUri(), def.getNamespace());
        }        

        if (!this.namespacePrefixMap.containsKey(def.getNamespace().getPrefix())) {            
            logger.info("Adding namespace prefix mapping: " + def.getNamespace().getPrefix()
                        + " -> " + def.getNamespace());
            this.namespacePrefixMap.put(def.getNamespace().getPrefix(), def.getNamespace());
        }
        
        Map propDefMap = (Map)this.propertyTypeDefinitions.get(namespace);
        
        if (propDefMap == null) {
            propDefMap = new HashMap();
            // XXX: what about prefix when using namespaces as map keys?
            this.propertyTypeDefinitions.put(namespace, propDefMap);
        }
        for (int u = 0; u < definitions.length; u++) {
            if (logger.isDebugEnabled()) {
                logger.debug("Registering property type definition "
                        + definitions[u].getName());
            }

            propDefMap.put(definitions[u].getName(), definitions[u]);
        }
    }

    public PropertyTypeDefinition findPropertyTypeDefinition(
            Namespace namespace, String name) {
        PropertyTypeDefinition propDef = null;
        Map map = (Map) this.propertyTypeDefinitions.get(namespace);

        if (map != null) {
            propDef = (PropertyTypeDefinition) map.get(name);
        }

        if (logger.isDebugEnabled() && propDef == null)
            logger.debug("No definition found for property "
                    + namespace.getPrefix() + ":" + name);

        return propDef;
    }

    public String getResourceTypeTreeAsString() {
        StringBuffer sb = new StringBuffer();
        printResourceTypes(sb, 0, this.rootResourceTypeDefinition);
        printMixinTypes(sb);
        return sb.toString();
    }

    private void printMixinTypes(StringBuffer sb) {

        sb.append("\n");
        for (Iterator i = this.mixins.iterator(); i.hasNext();) {
            MixinResourceTypeDefinition mixin = (MixinResourceTypeDefinition) i
                    .next();
            printResourceTypes(sb, 0, mixin);
            sb.append("\n");
        }
    }

    private void printResourceTypes(StringBuffer sb, int level,
            ResourceTypeDefinition def) {

        if (level > 0) {
            for (int i = 1; i < level; i++)
                sb.append("  ");
            sb.append("|\n");
            for (int i = 1; i < level; i++)
                sb.append("  ");
            sb.append("+--");
        }

        sb.append("[").append(def.getNamespace()).append("] ").append(
                def.getName());
        if (def instanceof MixinResourceTypeDefinition) {
            sb.append(" (mixin)");
        }
        sb.append("\n");

        MixinResourceTypeDefinition[] mixins = (MixinResourceTypeDefinition[]) this.mixinTypeDefinitionMap
                .get(def);
        if (mixins != null) {
            for (int i = 0; i < mixins.length; i++) {
                for (int j = 0; j < level; j++)
                    sb.append("  ");
                sb.append("  mixin: [");
                sb.append(mixins[i].getNamespace()).append("] ");
                sb.append(mixins[i].getName()).append("\n");
            }
        }

        PropertyTypeDefinition[] definitions = def.getPropertyTypeDefinitions();
        if (definitions.length > 0) {
            for (int i = 0; i < definitions.length; i++) {
                sb.append("  ");
                for (int j = 0; j < level; j++)
                    sb.append("  ");
                String type = PropertyType.PROPERTY_TYPE_NAMES[definitions[i]
                        .getType()];
                sb.append(type);
                if (definitions[i].isMultiple())
                    sb.append("[]");
                sb.append(" ").append(definitions[i].getName());
                sb.append("\n");
            }
        }
        PrimaryResourceTypeDefinition[] children = (PrimaryResourceTypeDefinition[]) this.resourceTypeDefinitions
                .get(def);
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                printResourceTypes(sb, level + 1, children[i]);
            }
        }
    }



    
    private  void init() {
        Collection resourceTypeDefinitionBeans = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values();

        PrimaryResourceTypeDefinition rootDefinition = null;
        for (Iterator i = resourceTypeDefinitionBeans.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            if (def.getParentTypeDefinition() == null) {
                if (rootDefinition != null) {
                    throw new IllegalStateException(
                        "Only one PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null may be defined");
                }
                rootDefinition = def;
            }
        }
        if (rootDefinition == null) {
                    throw new IllegalStateException(
                        "A PrimaryResourceTypeDefinition having "
                        + "parentTypeDefinition = null must be defined");
        }
        this.rootResourceTypeDefinition = rootDefinition;
        
        for (Iterator i = resourceTypeDefinitionBeans.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            
            this.resourceTypeNameMap.put(def.getName(), def);
            if (def.getNamespace() == null) {
                throw new BeanInitializationException(
                    "Definition's namespace is null: " + def
                    + " (already initialized resourceTypes = " + this.resourceTypeNameMap + ")");
            }

            addNamespacesAndProperties(def);
            
            // Populate map of resourceTypeDefiniton parent -> children
            PrimaryResourceTypeDefinition parent = def.getParentTypeDefinition();
            PrimaryResourceTypeDefinition[] children = 
                    (PrimaryResourceTypeDefinition[]) this.resourceTypeDefinitions.get(parent);

            // Array append (or create if not exists for given parent)
            PrimaryResourceTypeDefinition[] newChildren = null;

            if (children == null) {
                newChildren = new PrimaryResourceTypeDefinition[1];
                newChildren[0] = def;
            } else {
                newChildren = new PrimaryResourceTypeDefinition[children.length + 1];
                System.arraycopy(children, 0, newChildren, 0, children.length);
                newChildren[newChildren.length - 1] = def;
            }

            this.resourceTypeDefinitions.put(parent, newChildren);
            this.mixinTypeDefinitionMap.put(def, buildMixinTypes(def));

        }
        
        // Remove null-key (which is the root resource type's "parent")
        this.resourceTypeDefinitions.remove(null);
        
        this.mixins = 
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    MixinResourceTypeDefinition.class, false, false).values();
        for (Iterator iter = this.mixins.iterator(); iter.hasNext();) {
            ResourceTypeDefinition def = (ResourceTypeDefinition) iter.next();
            this.resourceTypeNameMap.put(def.getName(), def);
            addNamespacesAndProperties(def);
        }

        logger.info("Resource type tree: \n" + getResourceTypeTreeAsString());
    }

    private MixinResourceTypeDefinition[] buildMixinTypes(ResourceTypeDefinition rt) {
        List mixinTypes = new ArrayList();
        MixinResourceTypeDefinition[] directMixins = rt.getMixinTypeDefinitions();
        if (directMixins != null) {
            for (int i = 0; i < directMixins.length; i++) {
                MixinResourceTypeDefinition mix = directMixins[i];
                mixinTypes.add(mix);
                if (!this.namespaceUriMap.containsKey(mix.getNamespace().getUri()))
                    this.namespaceUriMap.put(mix.getNamespace().getUri(), mix.getNamespace());                    

                MixinResourceTypeDefinition[] indirectMixins =
                    directMixins[i].getMixinTypeDefinitions();
                if (indirectMixins != null && indirectMixins.length > 0) {
                    mixinTypes.addAll(Arrays.asList(buildMixinTypes(indirectMixins[i])));
                }
            }
        }        
        return (MixinResourceTypeDefinition[]) mixinTypes.toArray(
            new MixinResourceTypeDefinition[mixinTypes.size()]);
    }
    
    /* Build map of resource type names to names of all descendants */
    /* XXX: mixin types */
    private Map buildResourceTypeDescendantsMap() {
        List definitions = getPrimaryResourceTypeDefinitions();
        
        Map resourceTypeDescendantNames = new HashMap();
        
        for (Iterator i = definitions.iterator(); i.hasNext();) {
            PrimaryResourceTypeDefinition def = (PrimaryResourceTypeDefinition)i.next();
            List descendantNames = new LinkedList();
            getAllDescendantNames(descendantNames, def);
            resourceTypeDescendantNames.put(def.getName(), descendantNames);
        }
        
        if (logger.isDebugEnabled()) {
            for (Iterator i=resourceTypeDescendantNames.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                String name = (String)entry.getKey();
                List descendantNames = (List)entry.getValue();
                
                StringBuffer buf = new StringBuffer("Descendant resource types of [" + name + "]: [");
                for (Iterator u = descendantNames.iterator();u.hasNext();) {
                    buf.append(u.next());
                    if (u.hasNext()) {
                        buf.append(", ");
                    }
                }
                buf.append("]");
                logger.debug(buf.toString());
            }
        }        
        return resourceTypeDescendantNames;
    }

    /* Recursively get all descendant names for a given resource type */
    private void getAllDescendantNames(List names, PrimaryResourceTypeDefinition def) {
        List children = getResourceTypeDefinitionChildren(def);
        
        for (Iterator i=children.iterator();i.hasNext();) {
            PrimaryResourceTypeDefinition child = 
                (PrimaryResourceTypeDefinition)i.next();
            names.add(child.getName());
            getAllDescendantNames(names, child);
        }
    }


    public void afterPropertiesSet() throws Exception {
        init();
        this.resourceTypeDescendantNames = buildResourceTypeDescendantsMap();

    }

    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
                this.applicationContext = applicationContext;
        
        
    }

    public PrimaryResourceTypeDefinition getRoot() {
        return this.rootResourceTypeDefinition;
    }

    public MixinResourceTypeDefinition[] getMixinTypes(ResourceTypeDefinition rt) {
        return (MixinResourceTypeDefinition[]) this.mixinTypeDefinitionMap.get(rt);
    }
    
    public List getPrimaryResourceTypeDefinitions() {
        return new ArrayList(
            BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                    PrimaryResourceTypeDefinition.class, false, false).values());
    }

    public List getResourceTypeDefinitionChildren(PrimaryResourceTypeDefinition def) {
        PrimaryResourceTypeDefinition[] children = 
            getResourceTypeDefinitionChildrenInternal(def);
        
        ArrayList childList = new ArrayList();
        if (children != null) {
            for (int i=0; i<children.length; i++) {
                childList.add(children[i]);
            }
        }
        
        return childList;
    }
    
    public List getResourceTypeDescendantNames(String resourceTypeName) {
        return (List) this.resourceTypeDescendantNames.get(resourceTypeName);
    }

    public ResourceTypeDefinition getResourceTypeDefinitionByName(String name) {
        ResourceTypeDefinition type = (ResourceTypeDefinition)
            this.resourceTypeNameMap.get(name);
        if (type == null) {
            throw new IllegalArgumentException(
                "No resource type of name '" + name + "' exists");
        }

        return type;
    }

    public PropertyTypeDefinition getPropertyDefinitionByPrefix(String prefix, String name) {
        Namespace namespace = (Namespace) this.namespacePrefixMap.get(prefix);
        if (namespace == null) {
            return null;
        }
        return findPropertyTypeDefinition(namespace, name);
    }

    /**
     * Search upwards in resource type tree, collect property type definitions
     * from all encountered resource type definitions including mixin resource types.
     * Assuming that mixin types can never have other mixin types attached.
     * 
     * If there are more than one occurence of the same property type definition
     * for the given resource type, only the first occurence in the resource type
     * tree is added to the returned list (upward direction).
     * 
     * @param def The <code>ResourceTypeDefinition</code> 
     * @return A <code>Set</code> of <code>PropertyTypeDefinition</code> instances.
     */
    public List getPropertyTypeDefinitionsForResourceTypeIncludingAncestors(
                                                    ResourceTypeDefinition def) {
        Set encounteredIds = new HashSet();
        List propertyTypes = new ArrayList();
        
        if (def instanceof MixinResourceTypeDefinition) {
            MixinResourceTypeDefinition mixinDef = (MixinResourceTypeDefinition)def;
            
            PropertyTypeDefinition[] propDefs = mixinDef.getPropertyTypeDefinitions();
            addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
        } else {
            // Assuming instanceof PrimaryResourceTypeDefinition
            PrimaryResourceTypeDefinition primaryDef = (PrimaryResourceTypeDefinition)def; 

            while (primaryDef != null) {
                PropertyTypeDefinition[] propDefs = primaryDef.getPropertyTypeDefinitions();
                addPropertyTypeDefinitions(encounteredIds, propertyTypes, propDefs);
                
                // Add any mixin resource types' property type defs
                MixinResourceTypeDefinition[] mixinDefs = primaryDef.getMixinTypeDefinitions();
                for (int i=0; i<mixinDefs.length; i++) {
                    addPropertyTypeDefinitions(encounteredIds, propertyTypes, 
                                                mixinDefs[i].getPropertyTypeDefinitions());
                }

                primaryDef = primaryDef.getParentTypeDefinition();
            }
        }
        
        return propertyTypes;
    }
    
    private void addPropertyTypeDefinitions(Set encounteredIds, 
            List propertyTypes, 
            PropertyTypeDefinition[] propDefs) {
        for (int i = 0; i < propDefs.length; i++) {
            String id = propDefs[i].getNamespace().getUri() + ":" + propDefs[i].getName();
            // Add only _first_ occurence of property type definition
            if (encounteredIds.add(id)) {
                propertyTypes.add(propDefs[i]);
            }
        }
    }

    public boolean isContainedType(ResourceTypeDefinition def, String resourceTypeName) {

        ResourceTypeDefinition type = (ResourceTypeDefinition)
            this.resourceTypeNameMap.get(resourceTypeName);
        if (!(type instanceof PrimaryResourceTypeDefinition)) {
            throw new IllegalArgumentException("Supplied argument '" + resourceTypeName
                                               + "'not a primary resource type");
        }

        if (type == null) {
            return false;
        }

        ResourceTypeDefinition parent = type;
        while (parent != null) {
            if (def instanceof MixinResourceTypeDefinition) {
                MixinResourceTypeDefinition[] mixins = parent.getMixinTypeDefinitions();
                for (int i = 0; i < mixins.length; i++) {
                    if (mixins[i].equals(def)) {
                        return true;
                    }
                }
            } else if (parent.equals(def)) {
                return true;
            }
            parent = ((PrimaryResourceTypeDefinition) parent).getParentTypeDefinition();
        }
        return false;
    }

    
    public List getPropertyTypeDefinitions() {
        ArrayList definitions = new ArrayList();
        
        for (Iterator i = this.propertyTypeDefinitions.values().iterator(); i.hasNext();) {
            Map propMap = (Map)i.next();
            definitions.addAll(propMap.values());
        }
        
        return definitions;
    }

    public Namespace getNamespace(String namespaceUrl) {
        Namespace namespace = (Namespace) this.namespaceUriMap.get(namespaceUrl);
        
        if (namespace == null) 
            namespace = new Namespace(namespaceUrl);
        return namespace;
    }

    private PrimaryResourceTypeDefinition[] getResourceTypeDefinitionChildrenInternal(
            PrimaryResourceTypeDefinition rt) {

            PrimaryResourceTypeDefinition[] children =
                (PrimaryResourceTypeDefinition[])this.resourceTypeDefinitions.get(rt);
            if (children == null)
                return new PrimaryResourceTypeDefinition[0];
            return children;
    }
        

}
