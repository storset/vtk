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
package org.vortikal.search;



// Property comparison condition(s)
public class PropertyComparator implements Condition {
    public static final int EQ = 0;
    public static final int LT = 1;
    public static final int GT = 2;
    public static final int LTE = 3;
    public static final int GTE = 4;
    public static final int LIKE = 5;


    public String operatorToString(int operator) {
        switch (operator) {
            case EQ:
                return "eq";
            case LT:
                return "lt";
            case LTE:
                return "lte";
            case GTE:
                return "gte";
            case LIKE:
                return "like";
            default:
                return "unknown operator";
        }
    }
    


    private int operator;
    private String namespace = null;
    private String name;
    private Object value;


    // Constructor for property comparison using "standard" namespace
    public PropertyComparator(int operator, String name, Object value) {

        this.operator = operator;
        this.name = name;
        this.value = value;
    }



    // Constructor for property comparison using custom namespace
    public PropertyComparator(int operator, String namespace,
                              String name, Object value) {

        this.operator = operator;
        this.namespace = namespace;
        this.name = name;
        this.value = value;
    }



    /**
     * Get the <code>Operator</code> value.
     *
     * @return an <code>int</code>
     */
    public final int getOperator() {
        return operator;
    }



    /**
     * Set the <code>Operator</code> value.
     *
     * @param newOperator The new Operator value.
     */
    public final void setOperator(final int newOperator) {
        this.operator = newOperator;
    }


    

    /**
     * Gets the value of namespace
     *
     * @return the value of namespace
     */
        public final String getNamespace() {
        return this.namespace;
    }



    /**
     * Sets the value of namespace
     *
     * @param argNamespace Value to assign to this.namespace
     */
    public final void setNamespace(final String argNamespace)  {
        this.namespace = argNamespace;
    }



    /**
     * Gets the value of name
     *
     * @return the value of name
     */
    public final String getName() {
        return this.name;
    }



    /**
     * Sets the value of name
     *
     * @param argName Value to assign to this.name
     */
    public final void setName(final String argName)  {
        this.name = argName;
    }



    /**
     * Gets the value of value
     *
     * @return the value of value
     */
    public final Object getValue() {
        return this.value;
    }



    /**
     * Sets the value of value
     *
     * @param argValue Value to assign to this.value
     */
    public final void setValue(final Object argValue)  {
        this.value = argValue;
    }
    

    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("property('");
        if (this.namespace != null) {
            s.append(this.namespace).append(": ");
        }
        s.append(this.name).append("') ");
        s.append(operatorToString(this.operator)).append(" '");
        s.append(this.value).append("'");
        return s.toString();
    }

}
