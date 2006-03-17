package org.vortikal.repositoryimpl;

import java.io.IOException;
import java.io.InputStream;

import org.vortikal.repository.resourcetype.Content;

public class ContentImpl implements Content {

    private InputStream inputStream;
    
    public ContentImpl(InputStream inputStream) {
        this.inputStream = inputStream;
    }
    
    public Object getContentRepresentation(Class clazz) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    public InputStream getContentInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
