/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.repository;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.vortikal.security.Principal;

public class RecoverableResource {

    private int id;
    private String trashUri;
    private int parentId;
    private String deletedByUid;
    private Date deletedTime;
    private boolean wasInheritedAcl;
    private String resourceType;
    private Principal deletedBy;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public String getName() {
        return this.trashUri.substring(this.trashUri.lastIndexOf("/") + 1, this.trashUri.length());
    }

    public String getTrashID() {
        return this.trashUri.substring(0, this.trashUri.indexOf("/"));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTrashUri() {
        return trashUri;
    }

    public void setTrashUri(String trashUri) {
        this.trashUri = trashUri;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getDeletedByUid() {
        return deletedByUid;
    }

    public void setDeletedByUid(String deletedByUid) {
        this.deletedByUid = deletedByUid;
    }

    public Date getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(Date deletedTime) {
        this.deletedTime = deletedTime;
    }

    public boolean wasInheritedAcl() {
        return wasInheritedAcl;
    }

    public void setWasInheritedAcl(boolean wasInheritedAcl) {
        this.wasInheritedAcl = wasInheritedAcl;
    }

    public Principal getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Principal deletedByPrincipal) {
        this.deletedBy = deletedByPrincipal;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getTrashID());
        sb.append(": " + this.getName());
        sb.append(", deleted by " + this.getDeletedByUid());
        sb.append(", deleted " + SDF.format(this.getDeletedTime()));
        return sb.toString();
    }

}
