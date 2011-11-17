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
package org.vortikal.repository.reporting;

import org.vortikal.repository.resourcetype.PropertyType;

/**
 * Finds all unique values for the property and returns a report with a list of
 * value<->frequency pairs.
 * 
 */
public class PropertyValueFrequencyQuery extends AbstractPropertyValueQuery {

    public static final int DEFAULT_LIMIT = 10;
    public static final int DEFAULT_MIN_VALUE_FREQUENCY = 1;
    public static final int LIMIT_UNLIMITED = -1;

    public static enum Ordering {
        ASCENDING_BY_FREQUENCY("FREQ_ASC"), DESCENDING_BY_FREQUENCY("FREQ_DESC"), ASCENDING_BY_PROPERTY_VALUE(
                "VALUE_ASC"), DESCENDING_BY_PROPERTY_VALUE("VALUE_DESC"), NONE("NONE");

        private String value;

        Ordering(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private int limit = LIMIT_UNLIMITED;
    private int minValueFrequency = DEFAULT_MIN_VALUE_FREQUENCY;
    private Ordering ordering = Ordering.NONE;
    private boolean caseInsensitive = false;

    public void setLimit(int limit) {
        if (limit < 0 && limit != LIMIT_UNLIMITED) {
            throw new IllegalArgumentException("Limit must be greater that zero or -1");
        }
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }

    public Ordering getOrdering() {
        return ordering;
    }

    public void setOrdering(Ordering ordering) {
        this.ordering = ordering;
    }

    public void setMinValueFrequency(int minValueFrequency) {
        if (minValueFrequency < 1) {
            throw new IllegalArgumentException("Minimum value frequency must be greater than or equal to 1");
        }
        this.minValueFrequency = minValueFrequency;
    }

    public int getMinValueFrequency() {
        return this.minValueFrequency;
    }

    @Override
    public int hashCode() {
        int code = 7 * 31;

        code = 31 * code + getPropertyTypeDefintion().hashCode();
        code = 31 * code + getScoping().hashCode();
        code = 31 * code + this.ordering.hashCode();
        code = 31 * code + this.limit;
        code = 31 * code + this.minValueFrequency;

        if (isCaseInsensitive()) {
            code = code + 11;
        }

        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || (this.getClass() != other.getClass())) {
            return false;
        }

        PropertyValueFrequencyQuery otherQuery = (PropertyValueFrequencyQuery) other;

        if (!getPropertyTypeDefintion().equals(otherQuery.getPropertyTypeDefintion()))
            return false;

        if (!getScoping().equals(otherQuery.getScoping()))
            return false;

        if (this.ordering != otherQuery.ordering)
            return false;

        if (this.limit != otherQuery.limit)
            return false;

        if (this.minValueFrequency != otherQuery.minValueFrequency)
            return false;

        if (this.caseInsensitive != otherQuery.isCaseInsensitive())
            return false;

        return true;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(getClass().getSimpleName());

        buffer.append('[');
        buffer.append("propDef = ").append(getPropertyTypeDefintion()).append(", ");
        buffer.append("scope = ").append(getScoping()).append(", ");
        buffer.append("ordering = ").append(this.ordering).append(", ");
        buffer.append("minValueFrequency = ").append(this.minValueFrequency).append(", ");
        buffer.append("limit = ").append(this.limit).append(']');

        return buffer.toString();
    }

    @Override
    public Object clone() {
        PropertyValueFrequencyQuery clone = new PropertyValueFrequencyQuery();
        clone.setPropertyTypeDefinition(getPropertyTypeDefintion());
        for (ReportScope scope : getScoping()) {
            clone.addScope((ReportScope) scope.clone());
        }

        clone.setLimit(this.limit);
        clone.setOrdering(this.ordering);
        clone.setMinValueFrequency(this.minValueFrequency);
        clone.setCaseInsensitive(isCaseInsensitive());
        
        return clone;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive && getPropertyTypeDefintion() != null
                && getPropertyTypeDefintion().getType().equals(PropertyType.Type.STRING);
    }
}
