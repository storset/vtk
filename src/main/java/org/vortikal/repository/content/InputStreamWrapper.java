package org.vortikal.repository.content;

import java.io.InputStream;

import org.vortikal.repository.Path;

public class InputStreamWrapper {
    private InputStream inputStream;
    private Path path;

    public InputStreamWrapper(InputStream content, Path path) {
        this.path = path;
        this.inputStream = content;
    }
    
    public InputStreamWrapper(InputStream content){
        this.inputStream = content;
    }
    
    public InputStream getInputStream() {
        return inputStream;
    }

    public Path getPath() {
        return path;
    }

}
