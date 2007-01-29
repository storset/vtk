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
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.View;

import org.vortikal.web.RequestContext;
import org.vortikal.web.servlet.BufferedResponse;

import org.vortikal.web.view.decorating.DecoratorRequest;

public class RssFeedComponent extends AbstractDecoratorComponent {

    private static Log logger = LogFactory.getLog(RssIncludeComponent.class);
    
    private String identifier = "feed";
    private View view;


    public void setView(View view) {
        this.view = view;
    }
    
    public String getIdentifier() {
        return this.identifier;
    }


    public String getRenderedContent(DecoratorRequest request) throws Exception {
        String result = null;
        try {
            String address = request.getParameter("address");
            if (address == null) {
                throw new IllegalArgumentException("Component parameter 'address' is required");
            }

            boolean includeLogo = "true".equals(request.getParameter("includeLogo"));
            boolean includeTitle = "true".equals(request.getParameter("includeTitle"));
            boolean includeDescription = "true".equals(request.getParameter("includeDescription"));
            boolean includePublishedDate = "true".equals(request.getParameter("includePublishedDate"));
            boolean includeUpdatedDate = "true".equals(request.getParameter("includeUpdatedDate"));

            Integer maxMsgs = new Integer(5);
            String numStr = request.getParameter("maxMsgs");
            if (numStr != null) {
                try {
                    maxMsgs = Integer.valueOf(numStr);
                } catch (Exception e) { }
            }

            XmlReader xmlReader = new XmlReader(new URL(address));
            SyndFeedInput input = new SyndFeedInput();

            SyndFeed feed = input.build(xmlReader);
            Map conf = new HashMap();
            conf.put("includeLogo", new Boolean(includeLogo));
            conf.put("includeTitle", new Boolean(includeTitle));
            conf.put("includeDescription", new Boolean(includeDescription));
            conf.put("includePublishedDate", new Boolean(includePublishedDate));
            conf.put("includeUpdatedDate", new Boolean(includeUpdatedDate));
            conf.put("maxMsgs", maxMsgs);

            Map model = new HashMap();

            model.put("feed", feed);
            model.put("dateFormatter", new DateFormatter());
            model.put("conf", conf);
            BufferedResponse tmpResponse = new BufferedResponse();

            this.view.render(model, RequestContext.getRequestContext().getServletRequest(), tmpResponse);

            if (logger.isDebugEnabled()) {
                logger.debug("Rendered wrapped view " + this.view + ". "
                             + "Character encoding was: "
                             + tmpResponse.getCharacterEncoding() + ", "
                             + "Content-Length was: " + tmpResponse.getContentLength());
            }

            result = new String(tmpResponse.getContentBuffer(), tmpResponse.getCharacterEncoding());
        } catch (Exception e) {
            logger.warn(e);
            return e.getMessage();
        }

        return result;
    }

    private class DateFormatter {
        private String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm";
        private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

        /**
         * Formats the provided date
         * @param pDate date to format
         * @return formatted date
         */
        public String formatDate(Date pDate) {
            String formattedDate = "no date present";
            if (pDate != null) {
                formattedDate = DATE_FORMAT.format(pDate);
            }
            return formattedDate;
        }
    }
}
