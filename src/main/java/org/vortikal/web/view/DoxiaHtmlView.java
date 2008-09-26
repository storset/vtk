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
package org.vortikal.web.view;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.HTML.Tag;

import org.apache.maven.doxia.module.xhtml.XhtmlSink;
import org.apache.maven.doxia.parser.AbstractParser;
import org.apache.maven.doxia.sink.Sink;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.View;
import org.vortikal.repository.Resource;

public class DoxiaHtmlView implements View {

    private String streamKey;
    private String resourceKey;
    private Class<AbstractParser> parserClass;
    private String contentType = "text/html";
    private String characterEncoding = "utf-8";
    
    public String getContentType() {
        return this.contentType + ";charset=" + this.characterEncoding;
    }

    @SuppressWarnings("unchecked")
    public void render(Map model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

    	String title = null;
        if (this.resourceKey != null) {
            Resource resource = (Resource) model.get(this.resourceKey);
            if (resource != null) {
            	title = resource.getTitle();
            }
        }

    	InputStream in = (InputStream) model.get(this.streamKey);
        if (in == null) {
            throw new IllegalStateException("No input stream in model with key '" 
                                            + this.streamKey + "'");
        }
        
        Reader source = new InputStreamReader(in, this.characterEncoding);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Sink htmlSink = new InsertHeaderSink(new OutputStreamWriter(result, this.characterEncoding), title);

        AbstractParser parser = this.parserClass.newInstance();
        parser.parse(source, htmlSink);
        
        htmlSink.flush();
        htmlSink.close();
        
        response.setContentType(this.contentType + ";charset=" + this.characterEncoding);
        ServletOutputStream out = response.getOutputStream();
        out.write(result.toByteArray());
        out.flush();
        out.close();
    }

    @Required public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public void setResourceKey(String resourceKey) {
    	this.resourceKey = resourceKey;
    }
    
    @Required public void setParserClass(Class<AbstractParser> parserClass) {
        this.parserClass = parserClass;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    private class InsertHeaderSink extends XhtmlSink {
    	private String title;
    	private boolean wroteTitle = false;
    	
    	public InsertHeaderSink(Writer writer, String title) {
    		super(writer);
    		this.title = title;
    	}
    	
    	public void head_() {
    	    if (!this.wroteTitle) {
    	        if (this.title != null) {
    	            writeStartTag(Tag.TITLE);
    	            write(this.title);
    	            writeEndTag(Tag.TITLE);
    	        }
    	    }
    	    super.head_();
    	}
    	
    	public void title() {
    	    super.title();
    	    this.wroteTitle = true;
    	}
    	
        public void body() {
        	super.body();
        	if (this.title != null) {
        		writeStartTag(Tag.H1);
        		write(this.title);
        		writeEndTag(Tag.H1);
        	}
        }
        
    }
    
}
