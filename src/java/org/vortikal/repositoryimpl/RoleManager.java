/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.repositoryimpl;

import java.util.ArrayList;
import java.util.List;


/**
 * This class keeps track of roles assigned to certain principals. Any
 * principal can be assigned a role. This is useful in cases where
 * special system users are needed, for example users that need read
 * access to all resources. Currently, two system roles are defined,
 * <code>ROOT</code>, which means "allowed everything" and
 * <code>READ_EVERYTHING</code>, i.e. read access to all resources.
 */
public class RoleManager {
    public static final int ROOT = 0;
    public static final int READ_EVERYTHING = 1;
    private List rootRole = new ArrayList();
    private List readEverythingRole = new ArrayList();

    public boolean hasRole(String principalName, int role) {
        if ((role != ROOT) && (role != READ_EVERYTHING)) {
            throw new IllegalArgumentException("Unknown role");
        }

        switch (role) {
        case ROOT:
            return rootRole.contains(principalName);

        case READ_EVERYTHING:
            return readEverythingRole.contains(principalName);

        default:
            return false;
        }
    }

    public List listPrincipals(int role) {
        if ((role != ROOT) && (role != READ_EVERYTHING)) {
            throw new IllegalArgumentException("Unknown role");
        }

        switch (role) {
        case ROOT:
            return new ArrayList(rootRole);

        case READ_EVERYTHING:
            return new ArrayList(readEverythingRole);

        default:
            return null;
        }
    }

    /**
     * @param readEverythingRole The readEverythingRole to set.
     */
    public void setReadEverythingRole(List readEverythingRole) {
        this.readEverythingRole = readEverythingRole;
    }

    /**
     * @param rootRole The rootRole to set.
     */
    public void setRootRole(List rootRole) {
        this.rootRole = rootRole;
    }
}
