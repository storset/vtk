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
import java.util.Set;


/**
 * Externalization of SQL queries.
 *
 * XXX: fix horrible method names
 *
 */
public class QueryProvider {

    private boolean optimizeAclCopy = false;
    

    /**
     * Set to <code>true</code> for databases that support joining on
     * updates, i.e. for PostgreSQL: <code>update table set column =
     * from ...</code>.
     */
    public void setOptimizeAclCopy(boolean optimizeAclCopy) {
        this.optimizeAclCopy = optimizeAclCopy;
    }
    

    public String getLoadResourceByUriPreparedStatement() {
        return "select r.* from VORTEX_RESOURCE r where r.uri = ?";
    }    

    public String getLoadPropertiesByResourceIdPreparedStatement() {
        return "select * from EXTRA_PROP_ENTRY where resource_id = ? "
             + "order by extra_prop_entry_id";
    }    

    public String getLoadResourceIdByUriPreparedStatement() {
        return "select resource_id from vortex_resource where uri = ?";
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
            + "where l.timeout >= ? and r.resource_id in ("
            + "select resource_id from VORTEX_RESOURCE where uri like ?)";
        return query;
    }

//    public String getLoadLockByResourceUriPreparedStatement() {
//        String query = "select * from VORTEX_LOCK where resource_id in ("
//            + "select resource_id from VORTEX_RESOURCE where uri = ?)";
//        return query;
//    }


    public String getLoadLocksByResourceUrisPreparedStatement(String[] uris) {

        String query = "select r.uri as uri, l.* from VORTEX_RESOURCE r "
            + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
            + "where l.timeout >= ? and r.uri in (";

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
            + "guessed_character_encoding = ?, "
            + "user_specified_character_encoding = ?, "
            + "creation_time = ?, " + "resource_type = ?, " 
            + "content_length = ?, " + "created_by = ?, "
            + "modified_by = ?, " + "last_modified = ? "
            + "where uri = ?";
        return stmt;
    }


    public String getInsertResourcePreparedStatement() {
        String statement = "insert into VORTEX_RESOURCE "
            + "(resource_id, uri, resource_type, content_length, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, "
            + "resource_owner, display_name, "
            + "content_language, content_type, character_encoding, "
            + "guessed_character_encoding, user_specified_character_encoding, is_collection, "
            + "acl_inherited_from, created_by, modified_by, last_modified) "
            + "values (nextval('vortex_resource_seq_pk'), "
            + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return statement;
    }


    public String getDeleteLockByResourceIdPreparedStatement() {
        String query = "delete from VORTEX_LOCK where resource_id = ?";
        return query;
    }


    public String getLoadLockIdByTokenPreparedStatement() {
        String query = "select lock_id from VORTEX_LOCK where token = ?";
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


    public String getUpdateAclInheritedByResourceIdPreparedStatement() {
        return "update vortex_resource set acl_inherited_from = ? where resource_id = ?";
    }

    public String getUpdateAclInheritedByUriPreparedStatement() {
        return  "update vortex_resource set acl_inherited_from = ? "
            + "where uri = ? or uri like ?";
    }

    public String getUpdateAclInheritedByPrevIdPreparedStatement() {
        return  "update vortex_resource set acl_inherited_from = ? "
            + "where (uri = ? or uri like ?) and acl_inherited_from = ?";
        
    }
    


    public String getUpdateAclInheritedByResourceIdOrInheritedPreparedStatement() {
        return "update vortex_resource set acl_inherited_from = ? "
            + "where acl_inherited_from = ? or resource_id = ?";
    }

    public String getUpdateAclInheritedFromByInheritedPreparedStatement() {
        return  "update vortex_resource set acl_inherited_from = ? "
            + "where acl_inherited_from = ?";
    }

    public String getUpdateAclInheritedFromByPrevResourceIdPreparedStatement() {
        if (!this.optimizeAclCopy) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append("update vortex_resource set acl_inherited_from = r.resource_id ");
        sb.append("from vortex_resource r where ");
        sb.append("(vortex_resource.uri = ? or vortex_resource.uri like ?) and ");
        sb.append("r.prev_resource_id = vortex_resource.acl_inherited_from");
        return sb.toString();
    }
    
    public String getMapInheritedFromByPrevResourceIdPreparedStatement() {
        return "select r1.resource_id, r2.resource_id as inherited_from "
            + "from vortex_resource r1, vortex_resource r2 "
            + "where (r1.uri = ? or r1.uri like ?) "
            + "and r2.prev_resource_id = r1.acl_inherited_from";
    }

       
    public String getLoadActionTypeIdFromNamePreparedStatement() {
        return "select action_type_id from ACTION_TYPE where name = ?";
    }

    public String getLoadActionTypesPreparedStatement() {
        return "select * from ACTION_TYPE";
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
            + "(extra_prop_entry_id, resource_id, prop_type_id, name_space, name, value) "
            + "values (nextval('extra_prop_entry_seq_pk'), ?, ?, ?, ?, ?)";
    }


    public String getDeleteAclEntriesByUriPreparedStatement() {
        return "delete from ACL_ENTRY where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

    public String getDeleteLocksByUriPreparedStatement() {
        return "delete from VORTEX_LOCK where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

    public String getDeletePropertiesByUriPreparedStatement() {
        return "delete from EXTRA_PROP_ENTRY where resource_id in ("
            + "select resource_id from VORTEX_RESOURCE "
            + "where uri like ? or resource_id = ?)";
    }

    public String getDeleteResourcesByUriPreparedStatement() {
        return "delete from VORTEX_RESOURCE where uri = ? or uri like ?";
    }
    
       
    public String getLoadAclsByResourceIdsPreparedStatement(Set resourceIds) {

        StringBuffer query = new StringBuffer();

        query.append("select r.resource_id, a.*, ");
        query.append("t.name as action_name from ACL_ENTRY a ");
        query.append("inner join ACTION_TYPE t on a.action_type_id = t.action_type_id ");
        query.append("inner join VORTEX_RESOURCE r on r.resource_id = a.resource_id ");
        query.append("where r.resource_id in (");

        for (int i = 0; i < resourceIds.size(); i++) {
            query.append("?");
            if (i < resourceIds.size() - 1) {
                query.append(",");
            }
        }
        query.append(")");
        return query.toString();
    }

    public String getFindAclInheritedFromResourcesPreparedStatement(int n) {
        StringBuffer query = 
            new StringBuffer("select r.resource_id, r.uri "
                             + "from ACL_ENTRY a "
                             + "inner join VORTEX_RESOURCE r on r.resource_id = a.resource_id "
                             + "where r.uri in (");

        for (int i = 0; i < n; i++) {
            query.append((i < n - 1) ? "?, " : "?)");
        }
              
        return query.toString();
    }


    public String getLoadChildUrisPreparedStatement() {
        return  "select uri from vortex_resource where uri like ? and depth = ?";
    }


    public String getLoadChildrenPreparedStatement() {
        return "select * from vortex_resource where depth = ? and uri like ?";
    }

    public String getLoadPropertiesForChildrenPreparedStatement() {
        return "select * from EXTRA_PROP_ENTRY where resource_id in ("
            + "select resource_id from vortex_resource "
            + "where uri like ? and depth = ?) order by extra_prop_entry_id";
    }

    public String getLoadLocksForChildrenPreparedStatement() {
        return "select r.uri as uri, l.* from VORTEX_RESOURCE r "
            + "inner join VORTEX_LOCK l on r.resource_id = l.resource_id "
            + "where l.timeout >= ? and r.resource_id in (select resource_id " 
            + "from vortex_resource where uri like ? and depth = ?)";

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
            + "guessed_character_encoding, user_specified_character_encoding, "
            + "is_collection, acl_inherited_from, resource_type, content_length, " 
            + "created_by, modified_by, last_modified) "
            + "select nextval('vortex_resource_seq_pk'), resource_id, "
            + "? || substring(uri, length(?) + 1), "
            + "depth + ?, creation_time, content_last_modified, "
            + "properties_last_modified, " 
            + "content_modified_by, properties_modified_by, resource_owner, display_name, "
            + "content_language, content_type, character_encoding, guessed_character_encoding, "
            + "user_specified_character_encoding, is_collection, "
            + "acl_inherited_from, resource_type, content_length, " 
            + "created_by, modified_by, last_modified from vortex_resource "
            + "where uri = ? or uri like ?";
              
        return query;
              
    }

    public String getCopyResourceSetOwnerPreparedStatement() {

        String query = "insert into vortex_resource (resource_id, prev_resource_id, "
            + "uri, depth, creation_time, content_last_modified, properties_last_modified, "
            + "content_modified_by, properties_modified_by, resource_owner, "
            + "display_name, content_language, content_type, character_encoding, "
            + "is_collection, acl_inherited_from, resource_type, content_length, "
            + "created_by, modified_by, last_modified) "
            + "select nextval('vortex_resource_seq_pk'), resource_id, "
            + "? || substring(uri, length(?) + 1), "
            + "depth + ?, creation_time, content_last_modified, "
            + "properties_last_modified, " 
            + "content_modified_by, properties_modified_by, ?, display_name, "
            + "content_language, content_type, character_encoding, guessed_character_encoding, "
            + "user_specified_character_encoding, is_collection, "
            + "acl_inherited_from, resource_type, content_length, "
            + "created_by, modified_by, last_modified from vortex_resource "
            + "where uri = ? or uri like ?";
              
        return query;
              
    }

       
    public String getCopyPropertiesPreparedStatement() {
        String query = "insert into extra_prop_entry (extra_prop_entry_id, "
            + "resource_id, prop_type_id, name_space, name, value) "
            + "select nextval('extra_prop_entry_seq_pk'), r.resource_id, p.prop_type_id, p.name_space, "
            + "p.name, p.value from vortex_resource r inner join extra_prop_entry p "
            + "on r.prev_resource_id = p.resource_id where r.uri = ?"
            + "or r.uri like ? and r.prev_resource_id is not null order by p.extra_prop_entry_id";

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
