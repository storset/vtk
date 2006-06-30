/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.query.security;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.repositoryimpl.query.BinaryFieldValueMapper;
import org.vortikal.repositoryimpl.query.DocumentMapper;

/**
 * Extract relevant security info from a Lucene 
 * {@link org.apache.lucene.document.Document}.
 * 
 * @author oyviste
 *
 */
public class LuceneResultSecurityInfo implements ResultSecurityInfo {

    private Document document;
    private int aclInheritedFrom;
    private int resourceId;
    private Integer aclNodeId;
    private boolean authorized = false; 
    private String ownerAsUserOrGroupName;
    
    public LuceneResultSecurityInfo(Document doc) {
        this.document = doc;
        this.aclInheritedFrom = BinaryFieldValueMapper.getIntegerFromStoredBinaryField(
                doc.getField(DocumentMapper.ACL_INHERITED_FROM_FIELD_NAME));
        
        this.resourceId = BinaryFieldValueMapper.getIntegerFromStoredBinaryField(
                doc.getField(DocumentMapper.ID_FIELD_NAME));
        
        if (this.aclInheritedFrom == PropertySetImpl.NULL_RESOURCE_ID) {
            this.aclNodeId =  new Integer(this.resourceId);
        } else {
            this.aclNodeId = new Integer(this.aclInheritedFrom);
        }
        
        Field f = doc.getField(DocumentMapper.STORED_BINARY_FIELD_PREFIX
                                                + PropertyType.OWNER_PROP_NAME);
        this.ownerAsUserOrGroupName = BinaryFieldValueMapper.getStringFromStoredBinaryField(f);
    }
    
    public Integer getAclNodeId() {
        return this.aclNodeId;
    }
    
    public int getAclInheritedFrom() {
        return this.aclInheritedFrom;
    }

    public int getResourceId() {
        return this.resourceId;
    }
    
    public String getOwnerAsUserOrGroupName() {
        return this.ownerAsUserOrGroupName;
    }

    public Document getDocument() {
        return this.document;
    }

    public boolean isAuthorized() {
        return this.authorized;
    }
    
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
}
