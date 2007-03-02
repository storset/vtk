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
package org.vortikal.web.view.decorating.components;

import com.sun.syndication.feed.synd.SyndFeed;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;

import org.vortikal.util.cache.ContentCache;
import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.DecoratorRequest;
import org.vortikal.web.view.decorating.DecoratorResponse;


/**
 * XXX: this class currently depends on the thread safety of the
 * SyndFeed implementation: if it turns out that it is not thread
 * safe, its data has to be extracted data to a custom bean after
 * fetching a feed.
 */
public class SyndicationFeedComponent extends AbstractDecoratorComponent {

    private static Log logger = LogFactory.getLog(SyndicationFeedComponent.class);
    private ContentCache cache;
    private View view;

    private String defaultDateFormat = "yyyy-MM-dd HH:mm";

    public void setView(View view) {
        this.view = view;
    }

    public void setContentCache(ContentCache cache) {
        this.cache = cache;
    }
    
    public void setDefaultDateFormat(String defaultDateFormat) {
        if (defaultDateFormat == null) {
            throw new IllegalArgumentException("Date format not valid");
        }
        this.defaultDateFormat = defaultDateFormat;
    }

    public void render(DecoratorRequest request, DecoratorResponse response)
        throws Exception {
        String address = request.getStringParameter("address");
        if (address == null) {
            throw new DecoratorComponentException(
                "Component parameter 'address' is required");
        }

        boolean includeLogo = "true".equals(request.getParameter("includeLogo"));
        boolean includeTitle = "true".equals(request.getParameter("includeTitle"));
        boolean includeDescription = "true".equals(request.getParameter(
                                                       "includeDescription"));
        boolean includePublishedDate = "true".equals(request.getParameter(
                                                         "includePublishedDate"));
        boolean includeUpdatedDate = "true".equals(request.getParameter(
                                                       "includeUpdatedDate"));

        String dateFormat = request.getStringParameter("dateFormat");
        if (dateFormat == null) {
            dateFormat = this.defaultDateFormat;
        }

        Integer maxMsgs = new Integer(5);
        String numStr = request.getStringParameter("maxMsgs");
        if (numStr != null) {
            try {
                maxMsgs = Integer.valueOf(numStr);
            } catch (Exception e) { }
        }

        SyndFeed feed = (SyndFeed) this.cache.get(address);

        Map conf = new HashMap();
        conf.put("includeLogo", new Boolean(includeLogo));
        conf.put("includeTitle", new Boolean(includeTitle));
        conf.put("includeDescription", new Boolean(includeDescription));
        conf.put("includePublishedDate", new Boolean(includePublishedDate));
        conf.put("includeUpdatedDate", new Boolean(includeUpdatedDate));
        conf.put("maxMsgs", maxMsgs);

        Map model = new HashMap();

        model.put("feed", feed);
        model.put("dateFormatter", new DateFormatter(dateFormat));
        model.put("conf", conf);
        BufferedResponse tmpResponse = new BufferedResponse();

        this.view.render(
            model, RequestContext.getRequestContext().getServletRequest(),
            tmpResponse);

        if (logger.isDebugEnabled()) {
            logger.debug("Rendered wrapped view " + this.view + ". "
                         + "Character encoding was: "
                         + tmpResponse.getCharacterEncoding() + ", "
                         + "Content-Length was: " + tmpResponse.getContentLength());
        }

        response.setCharacterEncoding(tmpResponse.getCharacterEncoding());
        OutputStream out = response.getOutputStream();
        out.write(tmpResponse.getContentBuffer());
        out.close();
    }


    private class DateFormatter {
        private SimpleDateFormat dateFormat; 

        public DateFormatter(String dateFormat) {
            this.dateFormat = new SimpleDateFormat(dateFormat);
        }
        
        public String formatDate(Date date) {
            String formattedDate = "no date present";
            if (date != null) {
                formattedDate = this.dateFormat.format(date);
            }
            return formattedDate;
        }
    }


}
