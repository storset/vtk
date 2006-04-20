package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;

/**
 * Implementation of {@link org.vortikal.repository.PropertySet}.
 * 
 * @author oyviste
 *
 */

public class PropertySetImpl implements PropertySet {

    protected String uri;
    protected String resourceType;
    protected Map propertyMap;
   
    public PropertySetImpl(String uri) {
        this.uri = uri;
        propertyMap = new HashMap();
    }
    
    public String getURI() {
        return this.uri;
    }

    public String getName() {
        if (uri.equals("/")) {
            return uri;
        } 
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void addProperty(Property property) {
        Map map = (Map) this.propertyMap.get(property.getNamespace());
        if (map == null) {
            map = new HashMap();
            propertyMap.put(property.getNamespace(), map);
        }
        map.put(property.getName(), property);
    }
 
    public void removeProperty(Namespace namespace, String name) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map != null) {
            map.remove(name);
        }
    }
    
    public Property getProperty(Namespace namespace, String name) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map == null) return null;
        
        return (Property) map.get(name);
    }

    public List getProperties(Namespace namespace) {
        Map map = (Map) this.propertyMap.get(namespace);
        if (map == null) return new ArrayList();
        return new ArrayList(map.values());
    }

    public List getProperties() {
        List props = new ArrayList();
        for (Iterator iter = this.propertyMap.values().iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            props.addAll(map.values());
        }
        return props;
    }

    public Object clone() throws CloneNotSupportedException {
        
        PropertySetImpl clone = new PropertySetImpl(this.uri);
        clone.resourceType = this.resourceType;
        
        for (Iterator i = getProperties().iterator(); i.hasNext(); ){
            Property prop = (Property)i.next();
            clone.addProperty((Property) prop.clone());
        }
        
        return clone;
    }
    
}
