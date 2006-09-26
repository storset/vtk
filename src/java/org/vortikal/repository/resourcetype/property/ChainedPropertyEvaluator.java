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
package org.vortikal.repository.resourcetype.property;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.resourcetype.Content;
import org.vortikal.repository.resourcetype.ContentModificationPropertyEvaluator;
import org.vortikal.repository.resourcetype.CreatePropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertiesModificationPropertyEvaluator;
import org.vortikal.security.Principal;


public class ChainedPropertyEvaluator
  implements CreatePropertyEvaluator, PropertiesModificationPropertyEvaluator,
             ContentModificationPropertyEvaluator {

    private Log logger = LogFactory.getLog(this.getClass());

    private CreatePropertyEvaluator[] createEvaluators;
    private ContentModificationPropertyEvaluator[] contentModificationEvaluators;
    private PropertiesModificationPropertyEvaluator[] propertiesModificationEvaluators;
    
    
    public void setPropertyEvaluators(Object[] propertyEvaluators) {
        if (propertyEvaluators == null || propertyEvaluators.length == 0) {
            throw new IllegalArgumentException("No property evaluators specified");
        }
        List createEvaluators = new ArrayList();
        List contentModificationEvaluators = new ArrayList();
        List propertiesModificationEvaluators = new ArrayList();

        for (int i = 0; i < propertyEvaluators.length; i++) {
            Object o = propertyEvaluators[i];
            if (o instanceof CreatePropertyEvaluator) {
                createEvaluators.add(o);
            }
            if (o instanceof ContentModificationPropertyEvaluator) {
                contentModificationEvaluators.add(o);
            }
            if (o instanceof PropertiesModificationPropertyEvaluator) {
                propertiesModificationEvaluators.add(o);
            }
        }
        
        this.createEvaluators = (CreatePropertyEvaluator[])
            createEvaluators.toArray(new CreatePropertyEvaluator[createEvaluators.size()]);

        this.contentModificationEvaluators = (ContentModificationPropertyEvaluator[])
            contentModificationEvaluators.toArray(
                new ContentModificationPropertyEvaluator[contentModificationEvaluators.size()]);

        this.propertiesModificationEvaluators = (PropertiesModificationPropertyEvaluator[])
            propertiesModificationEvaluators.toArray(
                new PropertiesModificationPropertyEvaluator[propertiesModificationEvaluators.size()]);
    }
    


    public boolean create(Principal principal, Property property,
                          PropertySet ancestorPropertySet, boolean isCollection,
                          Date time) throws PropertyEvaluationException {
        for (int i = 0; i < this.createEvaluators.length; i++) {
            if (this.createEvaluators[i].create(principal, property,
                                                ancestorPropertySet, isCollection, time)) {
                return true;
            }
        }
        return false;
    }


    public boolean propertiesModification(Principal principal, Property property,
                                          PropertySet ancestorPropertySet,
                                          Date time) throws PropertyEvaluationException {
        for (int i = 0; i < this.propertiesModificationEvaluators.length; i++) {
            if (this.propertiesModificationEvaluators[i].propertiesModification(
                    principal, property, ancestorPropertySet, time)) {
                return true;
            }
        }
        return false;
    }


    public boolean contentModification(Principal principal, Property property,
                                       PropertySet ancestorPropertySet, Content content,
                                       Date time) throws PropertyEvaluationException {
        for (int i = 0; i < this.contentModificationEvaluators.length; i++) {
            if (this.contentModificationEvaluators[i].contentModification(
                    principal, property, ancestorPropertySet, content, time)) {
                return true;
            }
        }
        return false;
    }
    




}
