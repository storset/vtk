package org.vortikal.repositoryimpl.query.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public abstract class AbstractMultipleQuery implements Query {

    private List queries = new ArrayList();

    public void add(Query query) {
        queries.add(query);
    }
    
    public List getQueries() {
        return queries;
    }

    public String dump(String prefix) {
        StringBuffer buf = new StringBuffer().append(prefix);
        buf.append(this.getClass().getName()).append("\n");
        
        prefix += "  ";
        for (Iterator iter = queries.iterator(); iter.hasNext();) {
            Query query = (Query) iter.next();
            buf.append(query.dump(prefix));
        }

        return buf.toString();
    }
}
