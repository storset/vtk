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
package org.vortikal.security;


/**
 * Package local implementation of {@link Principal}.
 */
class PrincipalImpl implements Principal, java.io.Serializable {

    private String name;
    private String qualifiedName;
    private String domain;
    

    public PrincipalImpl(String name, String qualifiedName, String domain) {
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.domain = domain;
    }
    

    public boolean equals(Object another) {
        // FIXME PLEASE!
        if (another instanceof Principal) {
            String anotherName = ((Principal)another).getQualifiedName();
            if ((getQualifiedName() == null &&  anotherName == null) || 
                (getQualifiedName() != null && getName().equals(anotherName))) {
                return true;
            }
        }   
        return false;
    }
    

    public int hashCode() {
        return name.hashCode();
    }
    

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return this.qualifiedName;
    }

    public String getDomain() {
        return this.domain;
    }
    

    public String toString() {
        return this.qualifiedName;
    }

    public int compareTo(Object o) {
        if (! (o instanceof Principal)) {
            throw new IllegalArgumentException(
                "Can only compare to other principal objects");
        }
        if (o == null) {
            throw new IllegalArgumentException(
                "Cannot compare to a null value");
        }
        return this.qualifiedName.compareTo(
            ((Principal) o).getQualifiedName());
    }
    
}
