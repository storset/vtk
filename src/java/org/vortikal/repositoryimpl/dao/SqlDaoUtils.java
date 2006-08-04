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
package org.vortikal.repositoryimpl.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.vortikal.repository.Namespace;
import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repositoryimpl.PropertyManager;
import org.vortikal.repositoryimpl.PropertySetImpl;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;


class SqlDaoUtils {

    public static String getUriSqlWildcard(String uri) {
        if ("/".equals(uri)) {
            return "/%";
        }
        return uri + "/%";
    }
    

    public static int getUriDepth(String uri) {
        if ("/".equals(uri)) {
            return 0;
        }
        int count = 0;
        for (int index = 0; (index = uri.indexOf('/', index)) != -1; count++, index ++);
        return count;
    }

    
    public static void populateStandardProperties(
        PropertyManager propertyManager, PrincipalFactory principalFactory,
        PropertySetImpl propertySet, ResultSet rs) throws SQLException {

        propertySet.setID(rs.getInt("resource_id"));
        
        boolean collection = rs.getString("is_collection").equals("Y");
        Property prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.COLLECTION_PROP_NAME,
            new Boolean(collection));
        propertySet.addProperty(prop);
        
        Principal createdBy = principalFactory.getUserPrincipal(rs.getString("created_by"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CREATEDBY_PROP_NAME,
                createdBy);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME,
            new Date(rs.getTimestamp("creation_time").getTime()));
        propertySet.addProperty(prop);

        Principal principal = principalFactory.getUserPrincipal(rs.getString("resource_owner"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.OWNER_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        String string = rs.getString("display_name");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.DISPLAYNAME_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = rs.getString("content_type");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTTYPE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = rs.getString("character_encoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = rs.getString("guessed_character_encoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_GUESSED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = rs.getString("user_character_encoding");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }
        
        string = rs.getString("content_language");
        if (string != null) {
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, 
                PropertyType.CONTENTLOCALE_PROP_NAME,
                string);
            propertySet.addProperty(prop);
        }

        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.LASTMODIFIED_PROP_NAME,
                new Date(rs.getTimestamp("last_modified").getTime()));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal(rs.getString("modified_by"));
        prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.MODIFIEDBY_PROP_NAME,
                principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLASTMODIFIED_PROP_NAME,
            new Date(rs.getTimestamp("content_last_modified").getTime()));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal(rs.getString("content_modified_by"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESLASTMODIFIED_PROP_NAME,
            new Date(rs.getTimestamp("properties_last_modified").getTime()));
        propertySet.addProperty(prop);

        principal = principalFactory.getUserPrincipal(rs.getString("properties_modified_by"));
        prop = propertyManager.createProperty(
            Namespace.DEFAULT_NAMESPACE, PropertyType.PROPERTIESMODIFIEDBY_PROP_NAME,
            principal);
        propertySet.addProperty(prop);

        if (!collection) {
            //long contentLength = contentStore.getContentLength(propertySet.getURI());
            long contentLength = rs.getLong("content_length");
            prop = propertyManager.createProperty(
                Namespace.DEFAULT_NAMESPACE, PropertyType.CONTENTLENGTH_PROP_NAME,
                new Long(contentLength));
            propertySet.addProperty(prop);
        }
        
        propertySet.setResourceType(rs.getString("resource_type"));

        int aclInheritedFrom =  rs.getInt("acl_inherited_from");
        if (rs.wasNull()) {
            aclInheritedFrom = -1;
        }
        propertySet.setAclInheritedFrom(aclInheritedFrom);
    }

    public static class PropHolder {
        String namespaceUri = "";
        String name = "";
        int type;
        int resourceId;
        List values;
        
        public boolean equals(Object object) {
            if (object == null) return false;
            
            if (object == this) return true;
            
            PropHolder other = (PropHolder) object;
            if (this.namespaceUri == null && other.namespaceUri != null ||
               this.namespaceUri != null && other.namespaceUri == null)
                return false;

            return ((this.namespaceUri == null && other.namespaceUri == null)
                    || (this.namespaceUri.equals(other.namespaceUri) &&
                        this.name.equals(other.name)                 &&
                        this.resourceId == other.resourceId));
        }
        
        public int hashCode() {
            if (this.namespaceUri == null) {
                return this.name.hashCode() + this.resourceId;
            }
            return this.namespaceUri.hashCode() 
                 + this.name.hashCode() + this.resourceId;
        }
    }

}
