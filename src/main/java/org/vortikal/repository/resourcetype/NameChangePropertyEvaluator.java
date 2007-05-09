package org.vortikal.repository.resourcetype;

import java.util.Date;

import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;

public interface NameChangePropertyEvaluator {

    boolean nameModification(Principal principal, Property property, Resource newResource, Date time);

}
