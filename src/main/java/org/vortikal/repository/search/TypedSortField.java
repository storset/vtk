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
 *  * Neither the type of the University of Oslo nor the names of its
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
package org.vortikal.repository.search;

import java.util.Locale;

import org.vortikal.repository.PropertySet;

/**
 * Typed sort fields are for special non-property repository types:
 * URI, name and resource type.
 */
public class TypedSortField extends AbstractSortField {

    private String type;
    
    public TypedSortField(String type) {
        validateType(type);
        this.type = type;
    }
    
    public TypedSortField(String type, SortFieldDirection direction) {
        super(direction);
        validateType(type);
        this.type = type;
    }
    
    public TypedSortField(String type, SortFieldDirection direction, Locale locale) {
        super(direction, locale);
        validateType(type);
        this.type = type;
    }

    private void validateType(String type) throws IllegalArgumentException {
        if (!(PropertySet.NAME_IDENTIFIER.equals(type)
                || PropertySet.URI_IDENTIFIER.equals(type)
                || PropertySet.TYPE_IDENTIFIER.equals(type))) {
            throw new IllegalArgumentException("Type must be one of "
                    + PropertySet.NAME_IDENTIFIER + ", "
                    + PropertySet.URI_IDENTIFIER + ", "
                    + PropertySet.TYPE_IDENTIFIER);
        }
    }

    public String getType() {
        return this.type;
    }
    
    @Override
    public String toString() {
        return this.type + " " + getDirection().toString();
    }
}
