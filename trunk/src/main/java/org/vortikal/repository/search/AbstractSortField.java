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
package org.vortikal.repository.search;

import java.util.Locale;

/**
 * XXX: Using hard-coded locale
 *
 */
public abstract class AbstractSortField implements SortField {

    private SortFieldDirection direction = SortFieldDirection.ASC;

    // XXX NO locale should not be hard-coded here.
    // Locale should default to be null, and client code should set it to something
    // else if necessary.
    private Locale locale = new Locale("no");

    public AbstractSortField(){}

    public AbstractSortField(SortFieldDirection direction) {
        this.direction = direction;
    }

    public AbstractSortField(SortFieldDirection direction, Locale locale) {
        this.direction = direction;
        this.locale = locale;
    }
    
    @Override
    public SortFieldDirection getDirection() {
        return this.direction;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractSortField other = (AbstractSortField) obj;
        if (this.direction != other.direction) {
            return false;
        }
        if (this.locale != other.locale && (this.locale == null || !this.locale.equals(other.locale))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.direction != null ? this.direction.hashCode() : 0);
        hash = 97 * hash + (this.locale != null ? this.locale.hashCode() : 0);
        return hash;
    }
    
}
