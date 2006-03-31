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

import java.util.List;


public class QueryProvider {


    public String getLoadResourceByUriPreparedStatement() {
        return "select r.* from VORTEX_RESOURCE r where r.uri = ?";
    }    

    public String getLoadPropertiesByResourceIdPreparedStatement() {
        return "select * from EXTRA_PROP_ENTRY where resource_id = ? "
             + "order by extra_prop_entry_id";
    }    

              
    public String getDeleteExpiredLocksPreparedStatement() {
        return "delete from VORTEX_LOCK where timeout < ?";
    }             


    public String getInsertRecursiveChangeLogEntryStatement(
        int resourceId, String uri, String uriWildcard, int id, int type, String operation) {

        String statement = "INSERT INTO changelog_entry "
            + "(changelog_entry_id, logger_id, logger_type, "
            + "operation, timestamp, uri, resource_id, is_collection) "
            + "select nextval('changelog_entry_seq_pk'), " + id + ", " + type + ", "
            + "'" + operation + "', now(), uri, ";
        if (resourceId == -1) {
            statement += "NULL, ";
        } else {
            statement += "resource_id, ";
        }
        statement += "is_collection from vortex_resource "
            + "where uri = '" + uri + "' or uri like '" + uriWildcard + "'";

        return statement;
    }

    public String getInsertChangeLogEntryPreparedStatement() {

        String statement = "INSERT INTO changelog_entry "
            + "(changelog_entry_id, logger_id, logger_type, "
            + "operation, timestamp, uri, resource_id, is_collection) "
            + "VALUES (nextval('changelog_entry_seq_pk'), ?, ?, ?, ?, ?, ?, ?)";

        return statement;
    }

    public String getDiscoverLocksByResourceIdPreparedStatement() {
        String query = "select r.uri, l.* from VORTEX_LOCK l inner join VORTEX_RESOURCE r "
            + "on l.resource_id = r.resource_id "
            + "where r.resource_id in ("
            + "select resource_id from VORTEX_RESOURCE where uri like ?)";
        return query;
    }

    public String getLoadLockByResourceUriPreparedStatement() {
        String query = "select * from VORTEX_LOCK where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE where uri = ?)";
        return query;
    }


    public String getLoadLocksByResourceUrisPreparedStatement(String[] uris) {

        String query = "select r.uri as uri, l.* from VORTEX_RESOURCE r "
            + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
            + "where r.uri in (";

        for (int i = 0; i < uris.length; i++) {
            query += ((i < (uris.length - 1)) ? "?, " : "?)");
        }
        return query;
    }

    public String getListSubTreePreparedStatement() {
        String query = "select uri from VORTEX_RESOURCE "
            + "where uri like ? order by uri asc";
        return query;
    }

    public String getUpdateResourcePreparedStatement() {
        String stmt = "update VORTEX_RESOURCE set "
            + "content_last_modified = ?, "
            + "properties_last_modified = ?, "
            + "content_modified_by = ?, "
            + "properties_modified_by = ?, " + "resource_owner = ?, "
            + "display_name = ?, " + "content_language = ?, "
            + "content_type = ?, " + "character_encoding = ?, "
            + "creation_time = ? " + "where uri = ?";
        return stmt;
    }


    public String getInsertResourcePreparedStatement() {
        String statement = "insert into VORTEX_RESOURCE "
            + "(resource_id, uri, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, "
            + "resource_owner, display_name, "
            + "content_language, content_type, character_encoding, is_collection, acl_inherited) "
            + "values (nextval('vortex_resource_seq_pk'), "
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return statement;
    }


    public String getDeleteLockByResourceIdPreparedStatement() {
        String query = "delete from VORTEX_LOCK where resource_id = ?";
        return query;
    }


    public String getLoadLockIdByTokenPreparedStatement() {
        String query = "select id from VORTEX_LOCK where token = ?";
        return query;
    }



    public String getLoadLockTypeIdFromNamePreparedStatement() {
        return "select lock_type_id from LOCK_TYPE where  name = ?";
    }


    public String getUpdateLockPreparedStatement() {
        String query = "update VORTEX_LOCK set "
            + "lock_type_id = ?, lock_owner = ?, lock_owner_info = ?, "
            + "depth = ?, timeout = ? " + "where token = ?";
        return query;
    }


    public String getInsertLockPreparedStatement() {
        String query = "insert into VORTEX_LOCK "
            + "(lock_id, token, resource_id, lock_type_id, lock_owner, "
            + "lock_owner_info, depth, timeout) "
            + "values (nextval('vortex_lock_seq_pk'), "
            + "?, ?, ?, ?, ?, ?, ?)";
        return query;
    }

    public String getDeleteACLEntryByResourceIdPreparedStatement() {
        return "delete from ACL_ENTRY where resource_id = ?";
    }


    public String getSetAclInheritedPreparedStatement(boolean inherited) {
        String flag = inherited ? "Y" : "N";
        String query = "update VORTEX_RESOURCE set acl_inherited = '" +
            flag + "' where resource_id = ?";
        return query;
       
    }

       
    public String getLoadActionTypeIdFromNamePreparedStatement() {
        return "select action_type_id from ACTION_TYPE where name = ?";
    }

    public String getInsertAclEntryPreparedStatement() {
        return "insert into ACL_ENTRY (acl_entry_id, action_type_id, "
            + "resource_id, user_or_group_name, "
            + "is_user, granted_by_user_name, granted_date) "
            + "values (nextval('acl_entry_seq_pk'), ?, ?, ?, ?, ?, ?)"; 
    }

       
    public String getDeletePropertiesByResourceIdPreparedStatement() {
        return "delete from EXTRA_PROP_ENTRY where resource_id = ?";
    }
       


    public String getInsertPropertyEntryPreparedStatement() {
        return "insert into EXTRA_PROP_ENTRY " 
            + "(extra_prop_entry_id, resource_id, name_space, name, value) "
            + "values (nextval('extra_prop_entry_seq_pk'), ?, ?, ?, ?)";
    }


    public String getDeleteAclEntriesByUriPreparedStatement() {
        return "delete from ACL_ENTRY where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }


    public String getDeleteLocksByUriPreparedStatement() {
        return "delete from ACL_ENTRY where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

    public String getDeletePropertiesByUriPreparedStatement() {
        return "delete from EXTRA_PROP_ENTRY where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

    public String getDeleteResourcesByUriPreparedStatement() {
        return "delete from VORTEX_RESOURCE where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

       
    public String getLoadAncestorAclsPreparedStatement(List uris) {
        StringBuffer query = 
            new StringBuffer("select r.uri, a.*, t.namespace as action_namespace, "
                             + "t.name as action_name from ACL_ENTRY a "
                             + "inner join ACTION_TYPE t on a.action_type_id = t.action_type_id "
                             + "inner join VORTEX_RESOURCE r on r.resource_id = a.resource_id "
                             + "where r.uri in (");

        int n = uris.size();
        for (int i = 0; i < n; i++) {
            query.append((i < n - 1) ? "?, " : "?)");
        }
              
        return query.toString();
    }


    public String getLoadChildUrisPreparedStatement() {
        return  "select uri from vortex_resource where uri like ? and depth = ?";
    }


    public String getLoadChildrenPreparedStatement() {
        return "select * from vortex_resource where uri like ? and depth = ?";
    }

    public String getLoadPropertiesForChildrenPreparedStatement() {
        return "select * from EXTRA_PROP_ENTRY where resource_id in ("
            + "select resource_id from vortex_resource "
            + "where uri like ? and depth = ?)";
    }

    public String getLoadLocksForChildrenPreparedStatement() {
        return "select r.uri as uri, l.* from VORTEX_RESOURCE r "
            + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
            + "where r.resource_id in (select resource_id from vortex_resource "
            + "where uri like ? and depth = ?)";

    }
       

    public String getDiscoverAclsPreparedStatement() {
        return "select distinct r.uri as uri from ACL_ENTRY a inner join VORTEX_RESOURCE r "
            + "on a.resource_id = r.resource_id "
            + "where r.resource_id in ("
            + "select resource_id  from VORTEX_RESOURCE where uri like ?)";

    }

       
    public String getCopyResourcePreserveOwnerPreparedStatement() {

        String query = "insert into vortex_resource (resource_id, prev_resource_id, "
            + "uri, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, resource_owner, "
            + "display_name, content_language, content_type, character_encoding, "
            + "is_collection, acl_inherited) "
            + "select nextval('vortex_resource_seq_pk'), resource_id, "
            + "? || substring(uri, length(?) + 1), "
            + "depth + ?, creation_time, content_last_modified, "
            + "properties_last_modified, " 
            + "content_modified_by, properties_modified_by, resource_owner, display_name, "
            + "content_language, content_type, character_encoding, is_collection, "
            + "acl_inherited from vortex_resource "
            + "where uri = ? or uri like ?";
              
        return query;
              
    }

    public String getCopyResourceSetOwnerPreparedStatement() {

        String query = "insert into vortex_resource (resource_id, prev_resource_id, "
            + "uri, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, resource_owner, "
            + "display_name, content_language, content_type, character_encoding, "
            + "is_collection, acl_inherited) "
            + "select nextval('vortex_resource_seq_pk'), resource_id, "
            + "? || substring(uri, length(?) + 1), "
            + "depth + ?, creation_time, content_last_modified, "
            + "properties_last_modified, " 
            + "content_modified_by, properties_modified_by, ?, display_name, "
            + "content_language, content_type, character_encoding, is_collection, "
            + "acl_inherited from vortex_resource "
            + "where uri = ? or uri like ?";
              
        return query;
              
    }

       
    public String getCopyPropertiesPreparedStatement() {
        String query = "insert into extra_prop_entry (extra_prop_entry_id, "
            + "resource_id, name_space, name, value) "
            + "select nextval('extra_prop_entry_seq_pk'), r.resource_id, p.name_space, "
            + "p.name, p.value from vortex_resource r inner join extra_prop_entry p "
            + "on r.prev_resource_id = p.resource_id where r.uri = ?"
            + "or r.uri like ? and r.prev_resource_id is not null";

        return query;
    }

    public String getCopyAclsPreparedStatement() {
        String query = "insert into acl_entry (acl_entry_id, resource_id, "
            + "action_type_id, user_or_group_name, is_user, granted_by_user_name, "
            + "granted_date) "
            + "select nextval('acl_entry_seq_pk'), r.resource_id, a.action_type_id, "
            + "a.user_or_group_name, a.is_user, a.granted_by_user_name, a.granted_date "
            + "from vortex_resource r inner join acl_entry a "
            + "on r.prev_resource_id = a.resource_id "
            + "where r.uri = ?  or r.uri like ? "
            + "and r.prev_resource_id is not null";

        return query;            
    }


    public String getClearPrevResourceIdPreparedStatement() {
        return "update vortex_resource set prev_resource_id = null "
            + "where uri = ? or uri like ?";
       }

    
    

}
