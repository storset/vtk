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
package org.vortikal.security;

public class PseudoPrincipal implements Principal {

    private static final long serialVersionUID = -5049099518204674971L;
    public static final String NAME_AUTHENTICATED = "pseudo:authenticated";
    public static final String NAME_ALL = "pseudo:all";
    public static final String NAME_OWNER = "pseudo:owner";
    
    public static PseudoPrincipal OWNER = 
        new PseudoPrincipal(NAME_OWNER);
    public static PseudoPrincipal ALL = 
        new PseudoPrincipal(NAME_ALL);
    public static PseudoPrincipal AUTHENTICATED = 
        new PseudoPrincipal(NAME_AUTHENTICATED);
    
    private String name;
    
    private PseudoPrincipal(String name) {
        this.name = name;
    }
    
    public static PseudoPrincipal getPrincipal(String name) {
        if (NAME_ALL.equals(name)) return ALL;
        if (NAME_AUTHENTICATED.equals(name)) return AUTHENTICATED;
        if (NAME_OWNER.equals(name)) return OWNER;
        throw new IllegalArgumentException("Pseudo principal with name '"
                + name + "' doesn't exist");
    }
    
    public String getName() {
        return this.name;
    }

    public String getUnqualifiedName() {
        return this.name;
    }

    public String getQualifiedName() {
        return this.name;
    }

    public String getDomain() {
        return null;
    }

    public String getURL() {
        return null;
    }

    public boolean isUser() {
        // XXX: Remove 
        return true;
    }

    public int getType() {
        return Principal.TYPE_PSEUDO;
    }

    public int compareTo(Object o) {
        // XXX: Auto-generated method stub
        return 0;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": [").append(this.name).append("]");
        return sb.toString();
    }
    
}
