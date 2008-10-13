package org.vortikal.repository.resourcetype.property;

import java.io.InputStream;
import java.util.Date;

import org.apache.log4j.Logger;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.security.Principal;

public class BinaryReferenceEvaluator implements ContentModificationPropertyEvaluator {
	
	private static final Logger log = Logger.getLogger(BinaryReferenceEvaluator.class);

	public boolean contentModification(Principal principal, Property property,
			PropertySet ancestorPropertySet, Content content, Date time) throws PropertyEvaluationException {
        try {
        	
        	InputStream in = content.getContentInputStream();
            property.setStringValue(String.valueOf(in.hashCode()));
            return true;
            
        } catch (Exception e) {
            log.warn("Unable to set binary reference for resource", e);
            return false;
        }
	}

}
