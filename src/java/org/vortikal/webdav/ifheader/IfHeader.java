package org.vortikal.webdav.ifheader;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Resource;

public interface IfHeader {

    public boolean matches(Resource resource);
    
    public boolean matchesEtags(Resource resource);
    
    public Iterator getAllTokens();

    public Iterator getAllNotTokens();

}
