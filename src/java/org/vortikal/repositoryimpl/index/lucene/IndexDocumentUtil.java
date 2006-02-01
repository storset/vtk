package org.vortikal.repositoryimpl.index.lucene;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.springframework.beans.BeanWrapperImpl;
import org.vortikal.repositoryimpl.index.FieldInfo;
import org.vortikal.repositoryimpl.index.FieldInfoProvidingBean;
import org.vortikal.repositoryimpl.index.IndexConstants;

/**
 * Utility class for creating index beans from Lucene documents, and vice-versa.
 * 
 * @author oyviste
 *
 */
public class IndexDocumentUtil {
    
    private static Log logger = LogFactory.getLog(IndexDocumentUtil.class);
    
    protected static Document createDocument(Object bean, Class beanClass, 
                                             String uri,  String parentIds) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating index document for resource '" + uri + "'");
        }
        
        BeanWrapperImpl wrapper = new BeanWrapperImpl(beanClass);
        wrapper.setWrappedInstance(bean);
        
        Document doc = new Document();
        // Add special (reserved) fields
        if (parentIds == null) {
            logger.warn("Unable to get parent IDs for resource '" + uri
                    + "' probably due to deleted parent.");
            return null;
        }
        
        // Add PARENTIDS field.
        if (logger.isDebugEnabled()) {
            logger.debug("Adding reserved field: " + IndexConstants.PARENT_IDS_FIELD
                    + " = " + parentIds);
        }
        doc.add(Field.Text(IndexConstants.PARENT_IDS_FIELD, parentIds));
        
        // Add CLASS field.
        if (logger.isDebugEnabled()) {
            logger.debug("Adding reserved field: " + IndexConstants.CLASS_FIELD +
                    " = " + beanClass.getName());
        }
        doc.add(Field.Keyword(IndexConstants.CLASS_FIELD, beanClass.getName()));
        
        // Add URI field.
        if (logger.isDebugEnabled()) {
            logger.debug("Adding reserved field: " + IndexConstants.URI_FIELD +
                    " = " + uri);
        }
        doc.add(Field.Keyword(IndexConstants.URI_FIELD, uri));

        // Add custom index bean fields
        PropertyDescriptor[] props = wrapper.getPropertyDescriptors();
        for (int i = 0; i < props.length; i++) {
            
            String name = props[i].getName();
            Object value = wrapper.getPropertyValue(name);
            
            if (value == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping bean property " + name + " (value = null)");
                }
                continue;
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug("Adding field: " + name + " = '" + value + "'"
                        + " to index for resource " + uri);
            }
            doc.add(createField(bean, name, value));
        }
        
        return doc;
    }
    
    protected static Field createField(Object bean, String name, Object value) {
        Field field = null;
        if (bean instanceof FieldInfoProvidingBean) {
            FieldInfo fieldInfo =
                    ((FieldInfoProvidingBean)bean).getFieldInfo(name);
            
            if (fieldInfo != null) {
                switch (fieldInfo.getFieldType()) {
                    case(FieldInfo.FIELDTYPE_DATE):
                        field = Field.Keyword(name, value.toString());
                        break;
                    case(FieldInfo.FIELDTYPE_KEYWORD):
                        field = Field.Keyword(name, value.toString());
                        break;
                    case(FieldInfo.FIELDTYPE_TEXT):
                        field = Field.Text(name, value.toString());
                        break;
                    case(FieldInfo.FIELDTYPE_TEXT_UNSTORED):
                        field = Field.UnStored(name, value.toString());
                        break;
                    case(FieldInfo.FIELDTYPE_TEXT_UNINDEXED):
                        field = Field.UnIndexed(name, value.toString());
                }
            }
        }

        if (field == null) {
            // Create field with defaults.
            field = new Field(name, value.toString(), true, true, false);
        }

        return field;
    }
    
    protected static Object createIndexBean(Document doc, Class beanClass) {
        Object instance = null;
        try {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(beanClass);
            instance = beanClass.newInstance();
            wrapper.setWrappedInstance(instance);
            PropertyDescriptor[] props = wrapper.getPropertyDescriptors();
            
            Map values = new HashMap();
            
            for (int u = 0; u < props.length; u++) {
                String name = props[u].getName();
                if (props[u].getWriteMethod() != null) {
                    values.put(name, doc.get(name));
                }
            }
            
            try {
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Attempting to populate bean "
//                            + instance + " with values "
//                            + values);
//                }
                wrapper.setPropertyValues(values);
                
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error setting properties", e);
                }
            }
            
//            if (logger.isDebugEnabled()) {
//                logger.debug("Instantiated object " + instance);
//            }

        }
        catch (InstantiationException ie) {
            logger.warn("InstantiationException while creating index bean object.", ie);
        }
        catch (IllegalAccessException iae) {
            logger.warn("IllegalAccessExcpetion while creating index bean object.", iae);
        }
        
        return instance;
    }
}
