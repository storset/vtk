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
package org.vortikal.web.decorating.components;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.text.html.EnclosingHtmlContent;
import org.vortikal.text.html.HtmlContent;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;
import org.vortikal.web.decorating.HtmlDecoratorComponent;

public class ElementInModelDecoratorComponent extends
        AbstractDecoratorComponent implements HtmlDecoratorComponent {

    private String modelKey;
    private boolean useEnclosedContent = false;
    
    @Override
    protected String getDescriptionInternal() {
        return null;
    }

    @Override
    protected Map<String, String> getParameterDescriptionsInternal() {
        return null;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
            throws Exception {
        List<HtmlContent> content = render(request);
        Writer out = response.getWriter();
        for (HtmlContent c: content) {
            if (c instanceof EnclosingHtmlContent) {
                out.write(((EnclosingHtmlContent) c).getEnclosedContent());
            } else {
                out.write(c.getContent());
            }
        }
    }

    public List<HtmlContent> render(DecoratorRequest request) throws Exception {
        @SuppressWarnings("rawtypes") Map model = request.getMvcModel();
        List<HtmlContent> result = new ArrayList<HtmlContent>();
        Object o = model.get(this.modelKey);
        if (o != null && o instanceof HtmlContent) {
            HtmlContent c = (HtmlContent) o;
            if (this.useEnclosedContent) {
                result.add(c);
            } else {
                if (c instanceof EnclosingHtmlContent) {
                    EnclosingHtmlContent e = (EnclosingHtmlContent) c;
                    result.addAll(Arrays.asList(e.getChildNodes()));
                } else {
                    result.add(c);
                }
            }
        }
        return result;
    }

    @Required public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }
    
    public void setUseEnclosedContent(boolean useEnclosedContent) {
        this.useEnclosedContent = useEnclosedContent;
    }

}
