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
package org.vortikal.repository.resourcetype.property;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.doxia.parser.AbstractParser;
import org.apache.maven.doxia.sink.SinkAdapter;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;

public class DoxiaTitleEvaluator implements PropertyEvaluator {

    private PropertyTypeDefinition characterEncodingPropDef;
    private Class<AbstractParser> parserClass;
    
    private static Log logger = LogFactory.getLog(DoxiaTitleEvaluator.class);

    
    @Required public void setCharacterEncodingPropertyDefinition(PropertyTypeDefinition characterEncodingPropDef) {
        this.characterEncodingPropDef = characterEncodingPropDef;
    }
    

    @Required public void setParserClass(Class<AbstractParser> parserClass) {
        this.parserClass = parserClass;
    }


    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {
        if (ctx.getContent() != null) {
            return false;
        }
        InputStream stream = null;
        String encoding = determineCharacterEncoding(ctx);
        
        try {
            stream = ctx.getContent().getContentInputStream();
            Reader source = new InputStreamReader(stream, encoding);
            TitleSink titleSink = new TitleSink();
            AbstractParser parser = this.parserClass.newInstance();
            parser.parse(source, titleSink);
            String title = titleSink.getTitle();
            if (title != null) {
                property.setStringValue(title);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.warn("Unable to evaluate title of APT resource '"
                        + ctx.getNewResource().getURI() + "'", e);
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) { }
            }
        }
    }
    
    private String determineCharacterEncoding(PropertyEvaluationContext ctx) {
        String encoding = null;
        if (this.characterEncodingPropDef == null) {
            return java.nio.charset.Charset.defaultCharset().toString().toLowerCase();
        }

        Property encProperty = ctx.getNewResource().getProperty(this.characterEncodingPropDef);
        if (encProperty != null) {
            try {
                encoding = encProperty.getStringValue();
                java.nio.charset.Charset.forName(encoding);
            } catch (Exception e) { }
        }
        if (encoding == null) {
            return java.nio.charset.Charset.defaultCharset().toString().toLowerCase();
        }

        return encoding;
    }
    

    private class TitleSink extends SinkAdapter {

        boolean inTitle = false;
        String title = null;
        
        @Override
        public void title() {
            super.title();
            this.inTitle = true;
        }

        @Override
        public void title_() {
            super.title_();
            this.inTitle = false;
        }

        @Override
        public void text(String text) {
            if (this.inTitle) {
                this.title = text;
            }
            super.text(text);
        }

        public String getTitle() {
            return this.title;
        }
    }

}
