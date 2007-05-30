/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.xml.xpath;

import java.util.List;
import java.util.Locale;
import org.jaxen.Context;
import org.jaxen.FunctionCallException;
import org.springframework.context.MessageSource;


// vrtx:localize(//title, 'My title')
public class ValueTransformingXPathFunction extends AbstractXPathFunction {

    private MessageSource messageSource;
    
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public Object call(Context context, List args) throws FunctionCallException {

        if (args.isEmpty() || args.size() != 1) {
            throw new FunctionCallException("This function takes one argument");
        }
        Object val = args.get(0);
        String stringVal = val.toString();
        
        
        try {
            System.out.println("__variable: " + context.getVariableValue("vrtx", "vrtx", "property"));
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return messageSource.getMessage(stringVal, new Object[0], Locale.getDefault());
    }
        
}

