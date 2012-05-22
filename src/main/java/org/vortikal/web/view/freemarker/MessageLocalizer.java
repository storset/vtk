/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web.view.freemarker;

import java.util.Locale;

import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.util.repository.LocaleHelper;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;

public class MessageLocalizer implements TemplateHashModel {

    private String code;
    private String defaultMessage;
    private RequestContext springRequestContext;
    private TemplateSequenceModel args;
    private Locale preferredLocale;

    public MessageLocalizer(String code, String defaultMessage, TemplateSequenceModel args,
            RequestContext springRequestContext) {
        this(code, defaultMessage, args, springRequestContext, null);
    }

    public MessageLocalizer(String code, String defaultMessage, TemplateSequenceModel args,
            RequestContext springRequestContext, Locale preferredLocale) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.args = args;
        this.springRequestContext = springRequestContext;
        this.preferredLocale = preferredLocale;
    }

    public boolean isEmpty() {
        return false;
    }

    /**
     * Gets a localized message. The <code>key</code> parameter is ignored, and
     * the message is retrieved based on the values in the constructor of this
     * class.
     * 
     * @param key
     *            a <code>String</code> value
     * @return a <code>StringModel</code> containing the localized message.
     */
    public TemplateModel get(String key) throws TemplateModelException {

        String[] argsInternal = null;
        if (this.args != null && this.args.size() > 0) {
            argsInternal = new String[this.args.size()];
            for (int i = 0; i < this.args.size(); i++) {
                Object o = this.args.get(i);
                argsInternal[i] = o.toString();
            }
        }

        Locale messageLocalizationLocale = LocaleHelper.getMessageLocalizationLocale(this.preferredLocale);
        String msg = null;
        if (messageLocalizationLocale == null) {
            msg = this.springRequestContext.getMessage(this.code, argsInternal, this.defaultMessage);
        } else {
            msg = this.springRequestContext.getMessageSource().getMessage(this.code, argsInternal, this.defaultMessage,
                    messageLocalizationLocale);
        }

        if (msg != null) {
            msg = msg.trim();
        } else {
            msg = this.code;
        }

        return new StringModel(msg, new BeansWrapper());
    }
}
