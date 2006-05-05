package org.vortikal.repositoryimpl.queryparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Simple cached result set.
 * 
 * XXX: Result type is assumed to be a <code>PropertySet</code>, but might as well be
 * a <code>Resource</code> ? What should clients expect, etc ? 
 * 
 * @author ovyiste
 */
public class ResultSetImpl implements ResultSet {

    private List results;
    
    public ResultSetImpl(List results) {
        this.results = results;
    }
    
    public ResultSetImpl() {
        results = new ArrayList();
    }
    
    public Object getResult(int index) {
        return results.get(index);
    }

    public List getResults(int maxIndex) {
        int max = Math.min(maxIndex, this.results.size());
        List list = new ArrayList();
        for (int i=0; i<max; i++) {
            list.add(results.get(i));
        }
        return list;
    }

    public List getAllResults() {
        return Collections.unmodifiableList(this.results);
    }

    public int getSize() {
        return results.size();
    }
    
    public void removeResult(int index) {
        this.results.remove(index);
    }
    
    public Iterator iterator() {
        return Collections.unmodifiableList(this.results).iterator();
    }

}
