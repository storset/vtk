package org.vortikal.web;

import org.vortikal.repository.Resource;

public class IndexFileResolver {

    private String[] indexFileList = new String[0];

    
    public String getIndexFile(Resource resource) {
        if (resource == null || !resource.isCollection()) {
            return null;
        }

        // XXX: optimize:
        String[] childURIs = resource.getChildURIs();
        for (int i = 0; i < this.indexFileList.length; i++) {
            for (int j = 0; j < childURIs.length; j++) {
                String name = childURIs[j].substring(childURIs[j].lastIndexOf("/") + 1);
                if (this.indexFileList[i].equals(name)) {
                    return childURIs[j];
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
