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
package vtk.repository.search.query;

import vtk.repository.resourcetype.PropertyTypeDefinition;

public class PropertyPrefixQuery extends AbstractPropertyQuery {

    private final String term;
    private final TermOperator op;

    public PropertyPrefixQuery(PropertyTypeDefinition propertyDefinition, String term, TermOperator op) {
        super(propertyDefinition);
        this.term = term;
        this.op = op;
    }

    public String getTerm() {
        return this.term;
    }

    public TermOperator getOperator() {
        return this.op;
    }

    @Override
    public Object accept(QueryTreeVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
    
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getName()).append(": ");
        buf.append("propdef = ").append(getPropertyDefinition());
        buf.append("; prefix ").append(this.op).append(" '").append(this.term).append("'");
        
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PropertyPrefixQuery other = (PropertyPrefixQuery) obj;
        if ((this.term == null) ? (other.term != null) : !this.term.equals(other.term)) {
            return false;
        }
        if (this.op != other.op) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 97 * hash + (this.term != null ? this.term.hashCode() : 0);
        hash = 97 * hash + (this.op != null ? this.op.hashCode() : 0);
        return hash;
    }

    
}
