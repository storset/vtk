package org.vortikal.repository.index.mapping;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.search.PropertySelect;
import org.vortikal.security.Principal;

public interface DocumentMapper {

    /**
     * Map loaded {@link Document} to a {@link PropertySetImpl} instance.
     * 
     * @param doc
     * @return
     * @throws DocumentMappingException
     */
    public PropertySetImpl getPropertySet(Document doc)
        throws DocumentMappingException;
    
    /**
     * Get ACL read principal <em>names</em> from a document.
     * @param doc
     * @return
     * @throws DocumentMappingException
     */
    public Set<String> getACLReadPrincipalNames(Document doc)
        throws DocumentMappingException;
    
    /**
     * Map a {@link PropertySetImpl} instance to a {@link Document}.
     * 
     * @param propertySet
     * @return
     * @throws DocumentMappingException
     */
    public Document getDocument(PropertySetImpl propertySet, Set<Principal> aclReadPrincipals)
        throws DocumentMappingException;
    
    /**
     * Get a <code>FieldSelector</code> instance corresponding to
     * property selection in <code>PropertySelect</code> instance.
     * 
     * @param select
     * @return
     */
    public FieldSelector getDocumentFieldSelector(PropertySelect select);
    
}
