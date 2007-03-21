package org.vortikal.repository.search;


public interface Parser extends QueryParser {

    public Sorting parseSortString(String sortString);


    
}
