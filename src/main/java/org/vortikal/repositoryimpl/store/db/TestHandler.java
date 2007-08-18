package org.vortikal.repositoryimpl.store.db;

import java.util.List;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repositoryimpl.PropertySetImpl;

public class TestHandler implements PropertySetHandler {

    private int count = 0;

    
    public void handlePropertySet(PropertySet propertySet) {

        List<Property> properties = propertySet.getProperties();
        
        System.out.println((++count) + ", URI = " + propertySet.getURI());
        System.out.println(properties);
        System.out.println();
        
        
        PropertySetImpl impl = (PropertySetImpl)propertySet;
        
        System.out.println("Ancestor ids: ");
        for (int i=0; i<impl.getAncestorIds().length; i++) {
            System.out.print(impl.getAncestorIds()[i]);
            System.out.print(",");
        }
        System.out.println();
    }

}
