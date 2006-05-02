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
package org.vortikal.web.controller.properties;

import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.web.controller.AbstractSaveCancelCommand;
import java.util.Arrays;



public class PropertyEditCommand extends AbstractSaveCancelCommand {

    private PropertyTypeDefinition definition;
    private String[] possibleValues;
    private String value;
    
    private String namespace;
    private String name;
    

    public PropertyEditCommand(String submitURL, PropertyTypeDefinition definition, String value) {
        super(submitURL);
        this.definition = definition;
        this.value = value;
        this.namespace = definition != null ? definition.getNamespace().getUri() : null;
        this.name = definition != null ? definition.getName() : null;
    }

    public PropertyEditCommand(String submitURL, PropertyTypeDefinition definition, String value,
                               String[] possibleValues) {
        super(submitURL);
        this.definition = definition;
        this.value = value;
        this.possibleValues = possibleValues;
        this.namespace = definition != null ? definition.getNamespace().getUri() : null;
        this.name = definition != null ? definition.getName() : null;
    }

    public PropertyTypeDefinition getDefinition() {
        return this.definition;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String[] getPossibleValues() {
        return this.possibleValues;
    }
    
    public void clear() {
        this.definition = null;
        this.value = null;
        this.possibleValues = null;
    }

    /**
         * toString methode: creates a String representation of the object
         * @return the String representation
         * @author info.vancauwenberge.tostring plugin
    
         */
        public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("PropertyEditCommand[");
            buffer.append("definition = ").append(definition);
            if (possibleValues == null) {
                buffer.append(", possibleValues = ").append("null");
            } else {
                buffer.append(", possibleValues = ").append(Arrays.asList(possibleValues).toString());
            }
            buffer.append(", value = ").append(value);
            buffer.append(", namespace = ").append(namespace);
            buffer.append(", name = ").append(name);
            buffer.append("]");
            return buffer.toString();
        }
    
}

