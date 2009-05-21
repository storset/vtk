package org.vortikal.web;

import java.util.List;

import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;

public class IndexFileResolver {

    private String[] indexFileList = new String[0];
    
    public Path getIndexFile(Resource resource) {
        if (resource == null || !resource.isCollection()) {
            return null;
        }

        List<Path> childURIs = resource.getChildURIs();
        for (String indexFileName: this.indexFileList) {
            for (Path child: childURIs){
                if (indexFileName.equals(child.getName())){
                    return child;
                }
            }
        }
        
        return null;
    }


    public void setIndexFileList(String[] indexFileList) {
        if (indexFileList != null)
            this.indexFileList = indexFileList;
    }
}
