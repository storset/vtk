/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.index.dms;

import java.util.HashMap;
import java.util.Map;
import org.vortikal.index.FieldInfo;
import org.vortikal.index.FieldInfoProvidingBean;

/**
 *
 * @author oyviste
 */
public class DMSIndexBean implements FieldInfoProvidingBean {
    
    public static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    
    protected static Map fieldInfo;
    // Initialize index field information.
    // TODO: Need to figure out better way of doing this. It will probably go away
    //       further down the road, when proper types are introduced. Indexers will then
    //       decide how to best index a field, based on its type (or something like that).
    // TODO: The field _names_ are limited by the fact that they are standard Java bean
    //       properties. Should provide more flexibility, ie. for property indexing.
    static { 
        fieldInfo = new HashMap();
        fieldInfo.put("creationDate",
                new FieldInfo("creationDate", FieldInfo.FIELDTYPE_DATE));
        fieldInfo.put("lastModified",
                new FieldInfo("lastModified", FieldInfo.FIELDTYPE_DATE));
        fieldInfo.put("schemaId", 
                new FieldInfo("schemaId", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("owner",
                new FieldInfo("owner", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("lastModifiedBy",
                new FieldInfo("lastModifiedBy", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("contentType",
                new FieldInfo("contentType", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("encoding",
                new FieldInfo("encoding", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("contentLength",
                new FieldInfo("contentLength", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("davResourceType",
                new FieldInfo("davResourceType", FieldInfo.FIELDTYPE_KEYWORD));
        fieldInfo.put("vortexResourceType",
                new FieldInfo("vortexResourceType", FieldInfo.FIELDTYPE_KEYWORD));
    }

    public FieldInfo getFieldInfo(String fieldName) {
        return (FieldInfo) fieldInfo.get(fieldName);
    }

    // Bean properties
    private String creationDate;
    private String lastModified;
    private String schemaId;
    private String owner;
    private String lastModifiedBy;
    private String contentType;
    private String encoding;
    private String contentLength;
    private String davResourceType;
    private String vortexResourceType;
    
    /** Creates a new instance of DMSIndexBean */
    public DMSIndexBean() {
    }

    // Getters and setters
    public String getVortexResourceType() {
        return vortexResourceType;
    }

    public void setVortexResourceType(String vortexResourceType) {
        this.vortexResourceType = vortexResourceType;
    }
    
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentLength() {
        return contentLength;
    }

    public void setContentLength(String contentLength) {
        this.contentLength = contentLength;
    }

    public String getDavResourceType() {
        return davResourceType;
    }

    public void setDavResourceType(String davResourceType) {
        this.davResourceType = davResourceType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("DMSIndexBean[");
        buffer.append("creationDate='").append(creationDate).append("',");
        buffer.append("lastModified='").append(lastModified).append("',");
        buffer.append("schemaId='").append(schemaId).append("',");
        buffer.append("owner='").append(owner).append("',");
        buffer.append("lastModifiedBy='").append(lastModifiedBy).append("',");
        buffer.append("contentType='").append(contentType).append("',");
        buffer.append("encoding='").append(encoding).append("',");
        buffer.append("contentLength='").append(contentLength).append("',");
        buffer.append("davResourceType='").append(davResourceType).append("',");
        buffer.append("vortexResourceType='").append(vortexResourceType).append("']");
        
        return buffer.toString();
    }

}
