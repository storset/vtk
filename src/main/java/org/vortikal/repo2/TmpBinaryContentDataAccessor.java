package org.vortikal.repo2;

import org.vortikal.repository.store.BinaryContentDataAccessor;

/*
 * Exists only for compatibility reasons with old 'repository' package. 
 * 
 * TODO Remove reference to BinaryContentDataAccessor from valueFactory.
 * @deprecated
 */
public class TmpBinaryContentDataAccessor implements BinaryContentDataAccessor {

    // The real property store:
    private BinaryPropertyStore propStore;
    
    public String getBinaryMimeType(String binaryRef) {
        try {
            PropertyID propID = PropertyID.valueOf(binaryRef);
            TypedContentStream contentStream = this.propStore.retrieve(propID);
            return contentStream.getContentType();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public org.vortikal.repository.ContentStream getBinaryStream(String binaryRef) {
        try {
            PropertyID propID = PropertyID.valueOf(binaryRef);
            TypedContentStream cs = this.propStore.retrieve(propID);
            return new org.vortikal.repository.ContentStream(cs.getStream(), cs.getLength());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void setPropStore(BinaryPropertyStore propStore) {
        this.propStore = propStore;
    }
}
