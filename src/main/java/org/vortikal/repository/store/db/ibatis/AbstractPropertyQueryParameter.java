/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.store.db.ibatis;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract iBatis parameter helper class.
 */
public abstract class AbstractPropertyQueryParameter {

    private static final Map<String, String> VORTEX_RESOURCE_PROPERTY_COLUMNS =
                            new HashMap<String, String>();
    
    // XXX: hard-coded.
    static {
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("createdBy",                     "created_by");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("creationTime",                  "creation_time");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("owner",                         "resource_owner");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("contentType",                   "content_type");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("characterEncoding",             "character_encoding");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("guessedCharachterEncoding",     "guessed_character_encoding");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("userSpecifiedCharacterEncoding","user_character_encoding");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("contentLanguage",               "content_language");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("lastModified",                  "last_modified");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("modifiedBy",                    "modified_by");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("contentLastModified",           "content_last_modified");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("contentModifiedBy",             "content_modified_by");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("propertiesLastModified",        "properties_last_modified");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("propertiesModifiedBy",          "properties_modified_by");
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("contentLength",                 "content_length");
        
        // XXX: meta property, but can be very useful to support.
        VORTEX_RESOURCE_PROPERTY_COLUMNS.put("resourceType", "resource_type");
    }
    
    private String name;
    private String namespaceUri;

    public AbstractPropertyQueryParameter(String name, String namespaceUri) {
        this.name = name;
        this.namespaceUri = namespaceUri;
    }

    public String getName() {
        return this.name;
    }

    public String getNamespaceUri() {
        return this.namespaceUri;
    }

    public boolean isVortexResourceProperty() {
        return (getVortexResourcePropertyColumn() != null);
    }

    public String getVortexResourcePropertyColumn() {
        if (this.namespaceUri == null) {
            return VORTEX_RESOURCE_PROPERTY_COLUMNS.get(this.name);
        } else {
            return null;
        }
    }

}
