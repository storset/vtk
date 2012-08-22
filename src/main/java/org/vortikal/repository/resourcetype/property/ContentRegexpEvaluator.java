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

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.BeanInitializationException;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.Value;

/**
 * Evaluator that performs a regular expression match on the resource
 * content, evaluating a boolean property to "true" if the expression
 * matches.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>characterEncoding</code> - the character encoding used
 *   to build a string representation of the byte stream of the
 *   resource (default is <code>ascii</code>)
 *   <li><code>pattern</code> - the regular expression to match
 *   <li><code>regexpFlags</code> - flags to apply when compiling the
 *   regular expression (default is <code>Pattern.CASE_INSENSITIVE |
 *   Pattern.DOTALL</code>) (see {@link Pattern}
 * </ul>
 *
 */
public class ContentRegexpEvaluator implements PropertyEvaluator {

    private String characterEncoding = "ascii";
    private String pattern;
    private Pattern compiledPattern;
    private long maxLength = -1;
    private String evaluation = null;
    
    private int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

    public void setPattern(String value) throws Exception {
        this.pattern = value;
        this.compiledPattern = Pattern.compile(this.pattern, this.flags);
    }
    
    public void setRegexpFlags(int flags) {
        this.flags = flags;
    }
    
    public void setMaxLength(long maxLength) {
        this.maxLength = maxLength;
    }
    
    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }
    
    public void afterPropertiesSet() {
        if (this.pattern == null) {
            throw new BeanInitializationException(
                "JavaBean property 'pattern' not specified");
        }
        this.compiledPattern = Pattern.compile(this.pattern, this.flags);
        Charset.forName(this.characterEncoding);
    }
    
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (this.maxLength > 0 && ctx.getNewResource().getContentLength() > this.maxLength) {
            return false;
        }
        
        if (property.getDefinition().getType() == PropertyType.Type.BOOLEAN) {
            if (ctx.getContent() == null) {
                return false;
            }
            try {
                byte[] buffer = (byte[]) ctx.getContent().getContentRepresentation(byte[].class);
                String contentString = new String(buffer, this.characterEncoding);
                Matcher matcher = this.compiledPattern.matcher(contentString);
                if (!matcher.find()) {
                    return false;
                }
                property.setBooleanValue(true);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        if (property.getDefinition().getType() != PropertyType.Type.STRING) {
            throw new IllegalStateException("Can only evaluate to BOOLEAN or STRING types");
        }
        if (this.evaluation == null) {
            throw new IllegalStateException("For STRING types, an evaluation expression is required");
        }
        
        try {
            byte[] buffer = (byte[]) ctx.getContent().getContentRepresentation(byte[].class);
            String contentString = new String(buffer, this.characterEncoding);
            Matcher matcher = this.compiledPattern.matcher(contentString);
            
            Set<Value> values = new HashSet<Value>();
            
            while (matcher.find()) {
                StringBuilder s = new StringBuilder(this.evaluation);

                for (int i = 0; i < matcher.groupCount(); i++) {
                    String var = "$" + (i + 1);
                    int pos = -1;
                    while ((pos = s.indexOf(var)) != -1) {
                        int end = pos + var.length();
                        String sub = matcher.group(i + 1);
                        if (sub == null) sub = "";
                        s.replace(pos, end, sub);
                    }
                }
                values.add(new Value(s.toString(), PropertyType.Type.STRING));
            }
            
            if (values.isEmpty()) {
                return false;
            }

            if (!property.getDefinition().isMultiple()) {
                property.setValue(values.iterator().next());
                return true;
            }
            property.setValues(values.toArray(new Value[values.size()]));
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

}
