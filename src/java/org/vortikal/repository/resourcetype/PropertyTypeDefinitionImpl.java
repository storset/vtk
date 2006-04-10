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
package org.vortikal.repository.resourcetype;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;


public class PropertyTypeDefinitionImpl implements PropertyTypeDefinition, InitializingBean {

    private String name;
    private int type = PropertyType.TYPE_STRING;
    private boolean multiple = false;
    private String protectionLevel = PropertyType.PROTECTION_LEVEL_ACL_WRITE;
    private boolean mandatory = false;
    private Value defaultValue;
    private Constraint constraint;
    private CreatePropertyEvaluator createEvaluator;
    private ContentModificationPropertyEvaluator contentModificationEvaluator;
    private PropertiesModificationPropertyEvaluator propertiesModificationEvaluator;
    private PropertyValidator validator;
    
    public void afterPropertiesSet() {
        if (this.mandatory && this.defaultValue == null) {
                throw new BeanInitializationException(
                    "JavaBean property 'defaultValue'"
                    + "must be specified for mandatory property types");
            }
        }
    }
    

    public ContentModificationPropertyEvaluator getContentModificationEvaluator() {
        return contentModificationEvaluator;
    }

    public void setContentModificationEvaluator(
            ContentModificationPropertyEvaluator contentModificationEvaluator) {
        this.contentModificationEvaluator = contentModificationEvaluator;
    }

    public CreatePropertyEvaluator getCreateEvaluator() {
        return createEvaluator;
    }

    public void setCreateEvaluator(CreatePropertyEvaluator createEvaluator) {
        this.createEvaluator = createEvaluator;
    }

    public PropertiesModificationPropertyEvaluator getPropertiesModificationEvaluator() {
        return propertiesModificationEvaluator;
    }

    public void setPropertiesModificationEvaluator(
            PropertiesModificationPropertyEvaluator propertiesModificationEvaluator) {
        this.propertiesModificationEvaluator = propertiesModificationEvaluator;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
    }

    public boolean isMandatory() {
        return this.mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setDefaultValue(Value defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Value getDefaultValue() {
        return this.defaultValue;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtectionLevel() {
        return protectionLevel;
    }

    public void setProtectionLevel(String protectionLevel) {
        this.protectionLevel = protectionLevel;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public PropertyValidator getValidator() {
        return validator;
    }

    public void setValidator(PropertyValidator validator) {
        this.validator = validator;
    }
    
}
