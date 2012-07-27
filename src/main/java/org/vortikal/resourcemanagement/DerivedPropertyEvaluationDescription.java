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
package org.vortikal.resourcemanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DerivedPropertyEvaluationDescription {

    public enum Operator {
        EXISTS, TRUNCATE, ISTRUNCATED, LOCALIZED
    }

    private static final Map<String, Operator> OPERATORS = new HashMap<String, Operator>();
    static {
        OPERATORS.put("exists", Operator.EXISTS);
        OPERATORS.put("truncate", Operator.TRUNCATE);
        OPERATORS.put("istruncated", Operator.ISTRUNCATED);
        OPERATORS.put("localized", Operator.LOCALIZED);
    }

    private List<EvaluationElement> evaluationElements = new ArrayList<EvaluationElement>();

    public void addEvaluationElement(EvaluationElement evaluationElement) {
        if (this.evaluationElements == null) {
            this.evaluationElements = new ArrayList<EvaluationElement>();
        }
        this.evaluationElements.add(evaluationElement);
    }

    public List<EvaluationElement> getEvaluationElements() {
        return this.evaluationElements;
    }
    public static Operator getOperator(String name) {
        return OPERATORS.get(name);
    }

    public static class EvaluationElement {

        private boolean string;
        private String value;
        private Operator operator;

        public EvaluationElement(boolean string, String value, Operator operator) {
            this.string = string;
            this.value = value;
            this.operator = operator;
        }

        public boolean isString() {
            return string;
        }

        public String getValue() {
            return value;
        }
        
        public Operator getOperator() {
            return this.operator;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.string) {
                sb.append("\"").append(this.value).append("\"");
            } else {
                sb.append(this.value);
            }
            if (this.operator != null) {
                sb.append("?").append(this.operator);
            }
            return sb.toString();
        }
    }
    
    public String toString() {
        return this.evaluationElements.toString();
    }

}
