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
package org.vortikal.repository.resourcetype;

import java.util.ArrayList;
import java.util.List;


public final class PropertyType {

    /*
     * Property data types
     */ 
    
    public static final int TYPE_STRING = 0;
    public static final int TYPE_INT = 1;
    public static final int TYPE_LONG = 2;
    public static final int TYPE_DATE = 3;
    public static final int TYPE_BOOLEAN = 4;

    /*
     *  Protection levels
     */
    
    // Write privilege needed to edit prop
    public static final int PROTECTION_LEVEL_EDITABLE = 0;

    // Principals with admin permissions can edit property
    public static final int PROTECTION_LEVEL_PROTECTED = 1;

    // Only principals of the ROOT role or owners are allowed to set property
    public static final int PROTECTION_LEVEL_OWNER_EDITABLE = 2;
    
    // Only principals of the ROOT role  are allowed to set property
    public static final int PROTECTION_LEVEL_ROOT_EDITABLE = 3;

    //  Property is uneditable
    public static final int PROTECTION_LEVEL_UNEDITABLE = 4;
 
    
    /*
     * Special properties
     */
    public static final String DEFAULT_NAMESPACE_URI = null;
    
    public static final String COLLECTION_PROP_NAME = "collection";
    public static final String OWNER_PROP_NAME = "owner";
    public static final String CREATIONTIME_PROP_NAME = "creationTime";
    public static final String DISPLAYNAME_PROP_NAME = "displayName";
    public static final String CONTENTTYPE_PROP_NAME = "contentType";
    public static final String CHARACTERENCODING_PROP_NAME = "characterEncoding";
    public static final String CONTENTLOCALE_PROP_NAME = "contentLocale";
    public static final String CONTENTLASTMODIFIED_PROP_NAME = "contentLastModified";
    public static final String CONTENTMODIFIEDBY_PROP_NAME = "contentModifiedBy";
    public static final String PROPERTIESLASTMODIFIED_PROP_NAME = "propertiesLastModified";
    public static final String PROPERTIESMODIFIEDBY_PROP_NAME = "propertiesModifiedBy";
    public static final String CONTENTLENGTH_PROP_NAME = "contentLength";
    
    public static final String[] SPECIAL_PROPERTIES = 
        new String[] {COLLECTION_PROP_NAME, OWNER_PROP_NAME, 
        CREATIONTIME_PROP_NAME, DISPLAYNAME_PROP_NAME, CONTENTTYPE_PROP_NAME,
        CHARACTERENCODING_PROP_NAME, CONTENTLOCALE_PROP_NAME, 
        CONTENTLASTMODIFIED_PROP_NAME, CONTENTMODIFIEDBY_PROP_NAME,
        PROPERTIESLASTMODIFIED_PROP_NAME, PROPERTIESMODIFIEDBY_PROP_NAME, 
        CONTENTLENGTH_PROP_NAME};
    
}