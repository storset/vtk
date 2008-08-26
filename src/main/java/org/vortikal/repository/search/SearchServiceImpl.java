/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.search;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.search.query.PropertyExistsQuery;
import org.vortikal.repository.search.query.PropertyTermQuery;
import org.vortikal.repository.search.query.Query;
import org.vortikal.repository.search.query.TermOperator;

public class SearchServiceImpl implements SearchService {

    private Searcher searcher;
    private PropertyTypeDefinition propDef;

    public SortedSet<String> getKeywords() {
        SortedSet<String> keywords = new TreeSet<String>();
        
        Search search = new Search();
        Query query = new PropertyExistsQuery(propDef, false);
        search.setQuery(query);
        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        select.addPropertyDefinition(propDef);
        search.setPropertySelect(select);
        ResultSet resultSet = searcher.execute(null, search);

        for (Iterator<PropertySet> iter = resultSet.iterator(); iter.hasNext();) {
            Value[] values = iter.next().getProperty(propDef).getValues();
            for (int i = 0; i < values.length; i++) {
                keywords.add(values[i].getStringValue());
                
            }
        }
        
        return keywords;
    }

    public SortedSet<Path> getResourcesWithKeyword(String keyword) {
        SortedSet<Path> uris = new TreeSet<Path>();

        Search search = new Search();
        Query query = new PropertyTermQuery(propDef, keyword, TermOperator.EQ);
        search.setQuery(query);
        ConfigurablePropertySelect select = new ConfigurablePropertySelect();
        search.setPropertySelect(select);
        ResultSet resultSet = searcher.execute(null, search);
        for (Iterator<PropertySet> iter = resultSet.iterator(); iter.hasNext();) {
            uris.add(iter.next().getURI());
        }
        return uris;
    }
    

    public void setSearcher(Searcher searcher) {
        this.searcher = searcher;
    }
    
    public void setPropDef(PropertyTypeDefinition propDef) {
        this.propDef = propDef;
    }

}
