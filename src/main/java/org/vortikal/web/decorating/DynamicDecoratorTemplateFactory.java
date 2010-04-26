/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.decorating;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.resourcemanagement.view.tl.ComponentInvokerNodeFactory;
import org.vortikal.text.tl.DefineNodeFactory;
import org.vortikal.text.tl.DirectiveNodeFactory;
import org.vortikal.text.tl.IfNodeFactory;
import org.vortikal.text.tl.ListNodeFactory;
import org.vortikal.text.tl.ValNodeFactory;
import org.vortikal.text.tl.expr.Function;

public class DynamicDecoratorTemplateFactory implements TemplateFactory, InitializingBean {

    private Map<String, DirectiveNodeFactory> directiveHandlers;
    private Set<Function> functions = new HashSet<Function>();
    private ComponentResolver componentResolver;

    public Template newTemplate(TemplateSource templateSource) throws InvalidTemplateException {
        return new DynamicDecoratorTemplate(templateSource, this.componentResolver, this.directiveHandlers);
    }

    @Required public void setComponentResolver(ComponentResolver componentResolver) {
        this.componentResolver = componentResolver;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, DirectiveNodeFactory> directiveHandlers = new HashMap<String, DirectiveNodeFactory>();
        directiveHandlers.put("if", new IfNodeFactory());

        ValNodeFactory val = new ValNodeFactory();
        //val.addValueFormatHandler(PropertyImpl.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value.class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        //val.addValueFormatHandler(Value[].class, new PropertyValueFormatHandler(this.valueFormatterRegistry));
        directiveHandlers.put("val", val);

        ListNodeFactory list = new ListNodeFactory();
        list.setFunctions(this.functions);
        directiveHandlers.put("list", list);
        
        //directiveHandlers.put("resource-props", new ResourcePropsNodeFactory(this.repository));

        DefineNodeFactory def = new DefineNodeFactory();
        def.setFunctions(this.functions);
        directiveHandlers.put("def", def);

        //directiveHandlers.put("localized", new LocalizationNodeFactory(this.resourceModelKey));
        directiveHandlers.put("call", new ComponentInvokerNodeFactory(new DynamicDecoratorTemplate.ComponentSupport()));

        this.directiveHandlers = directiveHandlers;
    }
    
    public void setFunctions(Set<Function> functions) {
        if (functions == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.functions = functions;
    }
}
