/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.search.query;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

/**
 * Simple visitor which dumps the complete query tree to a string. 
 *
 */
public class DumpQueryTreeVisitor implements QueryTreeVisitor {

    private static final String DUMP_LEVEL_PREFIX = "  ";
    
    /**
     * @param andQuery The <code>AndQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String<code> representation of an AND query node and the complete
     *         subtree.
     */
    public Object visit(AndQuery andQuery, Object data) {
        if (data == null) data = "";

        StringBuilder buffer = new StringBuilder((String)data);
        buffer.append(andQuery.getClass().getName()).append("\n");
        
        for(Query subQuery: andQuery.getQueries()) {
            buffer.append(subQuery.accept(this, data + DUMP_LEVEL_PREFIX));            
        }
        
        return buffer.toString();
    }

    /**
     * @param andQuery The <code>OrQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of an OR query node and the complete
     *         subtree.
     */
    public Object visit(OrQuery orQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buffer = new StringBuilder((String)data);
        buffer.append(orQuery.getClass().getName()).append("\n");
        
        for(Query subQuery: orQuery.getQueries()) {
            buffer.append(subQuery.accept(this, data + DUMP_LEVEL_PREFIX));            
        }
        
        return buffer.toString();
    }

    public Object visit(UriSetQuery uriSetQuery, Object data) {
        if (data == null) data = "";
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(uriSetQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("URI set =").append(uriSetQuery.getUris()).append("\n");
        buf.append(", operator = ").append(uriSetQuery.getOperator());
        
        return buf.toString();
    }
    
    /**
     * @param andQuery The <code>NamePrefixQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>NamePrefixQuery</code> node.
     */
    public Object visit(NamePrefixQuery npQuery, Object data) {
        if (data == null) data = "";
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(npQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Term = ").append(npQuery.getTerm()).append("\n");

        return buf.toString();
    }

    /**
     * @param andQuery The <code>NameRangeQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>NameRangeQuery</code> node.
     */
    public Object visit(NameRangeQuery nrQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(nrQuery.getClass().getName()).append("\n");

        buf.append((String)data).append("fromTerm = '").append(nrQuery.getFromTerm());
        buf.append("', toTerm = '").append(nrQuery.getToTerm()).append("', inclusive = '");
        buf.append(nrQuery.isInclusive()).append("'\n");
        
        return buf.toString();
    }
    
    /**
     * @param andQuery The <code>NameTermQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>NameTermQuery</code> node.
     */
    public Object visit(NameTermQuery ntQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(ntQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Term = '").append(ntQuery.getTerm());
        buf.append("', operator = '").append(ntQuery.getOperator()).append("'\n");
        return buf.toString();
    }

    /**
     * @param andQuery The <code>NameWildcardQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>NameWildcardQuery</code> node.
     */
    public Object visit(NameWildcardQuery nwQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(nwQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Term = ").append(nwQuery.getTerm()).append("\n");
        return buf.toString();
    }

    /**
     * @param andQuery The <code>PropertyExistsQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>PropertyExistsQuery</code> node.
     */
    public Object visit(PropertyExistsQuery peQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(peQuery.getClass().getName()).append("\n");

        PropertyTypeDefinition def = peQuery.getPropertyDefinition();
        
        buf.append((String)data).append("Property namespace = ").append(def.getNamespace());
        buf.append(", name = ").append(def.getName()).append("\n");
        buf.append("Inverted: " + peQuery.isInverted());
        return buf.toString();
    }

    /**
     * @param andQuery The <code>PropertyPrefixQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>PropertyPrefixQuery</code> node.
     */
    public Object visit(PropertyPrefixQuery ppQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(ppQuery.getClass().getName()).append("\n");

        PropertyTypeDefinition def = ppQuery.getPropertyDefinition();
        
        buf.append((String)data).append("Property namespace = '").append(def.getNamespace());
        buf.append("', name = '").append(def.getName()).append("', term = '").append(ppQuery.getTerm());
        buf.append("', op = ").append(ppQuery.getOperator()).append('\n');
        
        return buf.toString();
    }

    /**
     * @param andQuery The <code>PropertyRangeQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>PropertyRangeQuery</code> node.
     */
    public Object visit(PropertyRangeQuery prQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(prQuery.getClass().getName()).append("\n");

        PropertyTypeDefinition def = prQuery.getPropertyDefinition();
        
        buf.append((String)data).append("Property namespace = '").append(def.getNamespace());
        buf.append("', name = '").append(def.getName()).append("'\n");

        buf.append((String)data).append("fromTerm = '").append(prQuery.getFromTerm());
        buf.append("', toTerm = '").append(prQuery.getToTerm()).append("', inclusive = '");
        buf.append(prQuery.isInclusive()).append("'\n");
        
        return buf.toString();
    }

    /**
     * @param andQuery The <code>PropertyTermQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>PropertyTermQuery</code> node.
     */
    public Object visit(PropertyTermQuery ptQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(ptQuery.getClass().getName()).append("\n");

        PropertyTypeDefinition def = ptQuery.getPropertyDefinition();
        
        buf.append((String)data).append("Property namespace = '").append(def.getNamespace());
        buf.append("', name = '").append(def.getName()).append("'");
        buf.append(", term = '").append(ptQuery.getTerm()).append("'");
        buf.append(", operator = '").append(ptQuery.getOperator().toString()).append("'");
        buf.append("\n");
        
        return buf.toString();
    }

    /**
     * @param andQuery The <code>PropertyWildcardQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>PropertyWildcardQuery</code> node.
     */
    public Object visit(PropertyWildcardQuery pwQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(pwQuery.getClass().getName()).append("\n");

        PropertyTypeDefinition def = pwQuery.getPropertyDefinition();
        
        buf.append((String)data).append("Property namespace = '").append(def.getNamespace());
        buf.append("', name = '").append(def.getName()).append("', term ").append(pwQuery.getOperator()).append(" '");
        buf.append(pwQuery.getTerm()).append("'\n");
        
        return buf.toString();
    }

    /**
     * @param andQuery The <code>TypeTermQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>TypeTermQuery</code> node.
     */
    public Object visit(TypeTermQuery ttQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(ttQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Operator = ").append(ttQuery.getOperator());
        buf.append(", term = ").append(ttQuery.getTerm()).append("\n");
        return buf.toString();
    }

    /**
     * @param andQuery The <code>UriDepthQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>UriDepthQuery</code> node.
     */
    public Object visit(UriDepthQuery udQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder dump = new StringBuilder((String)data);
        dump.append(udQuery.getClass().getName()).append("\n");
        dump.append((String)data).append("Depth = " + udQuery.getDepth()).append("\n");
        return dump.toString();
    }

    /**
     * @param andQuery The <code>UriPrefixQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>UriPrefixQuery</code> node.
     */
    public Object visit(UriPrefixQuery upQuery, Object data) {
        if (data == null) data = "";
        
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(upQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Uri = ").append(upQuery.getUri()).append("\n");

        return buf.toString();
    }

    /**
     * @param andQuery The <code>UriTermQuery</code> instance.
     * @param data A <code>String</code> with the base output prefix or <code>null</code>.
     * 
     * @return A <code>String</code> representation of a <code>UriTermQuery</code> node.
     */
    public Object visit(UriTermQuery utQuery, Object data) {
        if (data == null) data = "";

        StringBuilder buf = new StringBuilder((String)data);
        buf.append(utQuery.getClass().getName()).append("\n");
        
        buf.append((String)data).append("Operator = ").append(utQuery.getOperator());
        buf.append(", Uri = ").append(utQuery.getUri()).append("\n");
        return buf.toString();
    }

    public Object visit(ACLExistsQuery aclExistsQuery, Object data) {
        if (data == null) data = "";
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(aclExistsQuery.getClass().getName()).append("\n");
        buf.append(data).append("Inverted: " + aclExistsQuery.isInverted()).append("\n");

        return buf.toString();
    }

    public Object visit(ACLInheritedFromQuery aclIHFQuery, Object data) {
        if (data == null) data = "";
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(aclIHFQuery.getClass().getName()).append("\n");
        buf.append(data).append("Inverted: " + aclIHFQuery.isInverted()).append("\n");

        return buf.toString();
    }

    @Override
    public Object visit(ACLReadForAllQuery query, Object data) {
        if (data == null) data = "";
        StringBuilder buf = new StringBuilder((String)data);
        buf.append(query.getClass().getName()).append("\n");
        buf.append(data).append("Inverted: " + query.isInverted()).append("\n");

        return buf.toString();
    }
    
    
}
