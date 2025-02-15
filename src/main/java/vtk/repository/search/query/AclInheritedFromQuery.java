/* Copyright (c) 2009, University of Oslo, Norway
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
package vtk.repository.search.query;

import vtk.repository.Path;

public class AclInheritedFromQuery extends AbstractAclQuery {

    private final Path uri;

    /**
     * Construct a new ACL resource inheritance query node.
     * 
     * <p>The query shall match all resources which inherit their ACL from the
     * resource identified by the provided URI. If the resource at <code>uri</code>
     * does not have its own ACL, then the query will match nothing.
     * 
     * @param uri a resource path
     */
    public AclInheritedFromQuery(Path uri) {
        super(false);
        this.uri = uri;
    }

    /**
     * See constructor {@link AclInheritedFromQuery(vtk.repository.Path) }.
     * @see AbstractAclQuery#isInverted() 
     */
    public AclInheritedFromQuery(Path uri, boolean inverted) {
        super(inverted);
        this.uri = uri;
    }
    
    public Path getUri() {
        return this.uri;
    }
    
    @Override
    public Object accept(QueryTreeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        if (super.inverted) {
            sb.append(";uri!=").append(this.uri);
        } else {
            sb.append(";uri=").append(this.uri);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AclInheritedFromQuery other = (AclInheritedFromQuery) obj;
        if (this.uri != other.uri && (this.uri == null || !this.uri.equals(other.uri))) {
            return false;
        }
        if (super.inverted != other.inverted) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        hash = 47 * hash + (super.inverted ? 1 : 0);
        return hash;
    }

}
