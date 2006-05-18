package org.vortikal.repositoryimpl.query;

import java.util.Iterator;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repositoryimpl.query.query.PropertySortField;
import org.vortikal.repositoryimpl.query.query.SimpleSortField;
import org.vortikal.repositoryimpl.query.query.SortField;
import org.vortikal.repositoryimpl.query.query.SortFieldDirection;
import org.vortikal.repositoryimpl.query.query.Sorting;

/**
 * 
 * @author oyviste
 *
 */
public class SortBuilderImpl implements SortBuilder {

    public org.apache.lucene.search.Sort buildSort(Sorting sort) 
        throws SortBuilderException {

        org.apache.lucene.search.SortField[] luceneSortFields =
            new org.apache.lucene.search.SortField[sort.getSortFields().size()];
        
        int j=0;
        for (Iterator i = sort.getSortFields().iterator(); i.hasNext(); j++) {
            SortField f = (SortField)i.next();
            
            if (f instanceof SimpleSortField) {
                luceneSortFields[j]= buildSimpleSortField((SimpleSortField)f);
            } else if (f instanceof PropertySortField) {
                luceneSortFields[j] = buildPropertySortField((PropertySortField)f);
            } else {
                throw new SortBuilderException("Unknown sorting type");
            }
            
        }
        
        return new org.apache.lucene.search.Sort(luceneSortFields);
    }
    
    private org.apache.lucene.search.SortField buildSimpleSortField(
            SimpleSortField ssf) {
        
        org.apache.lucene.search.SortField sortField = 
            new org.apache.lucene.search.SortField(ssf.getName(), 
                    org.apache.lucene.search.SortField.STRING,
                    (ssf.getDirection() == SortFieldDirection.ASC ? false : true));
        
        return sortField;
        
    }
    
    private org.apache.lucene.search.SortField buildPropertySortField(
            PropertySortField f) {
     
        PropertyTypeDefinition def = f.getDefinition();
        SortFieldDirection d = f.getDirection();
        String fieldName = DocumentMapper.getFieldName(def);
        
        org.apache.lucene.search.SortField sortField =
            new org.apache.lucene.search.SortField(fieldName,
                    org.apache.lucene.search.SortField.STRING, // We do our own type encoding, and must use Lucene's string sorting type
                    (d == SortFieldDirection.ASC ? false : true));
                    
        return sortField;
    }

}
