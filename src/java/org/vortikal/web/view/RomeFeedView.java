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

package org.vortikal.web.view;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.View;
import org.vortikal.web.referencedata.ReferenceDataProvider;
import org.vortikal.web.referencedata.ReferenceDataProviding;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * This view renders a rssfeed based on a object in the model of type
 * {@link com.sun.syndication.feed.synd.SyndFeed} with key "romeFeed".
 * 
 * The feedType is specified in the feed object.
 *  
 * Configurable properties:
 * <ul>
 *  <li>none</li>
 * </ul>
 *
 * (This view is copied from noticeboard)
 * 
 */
public class RomeFeedView implements View, ReferenceDataProviding, InitializingBean {

    private ReferenceDataProvider[] referenceDataProviders;

    private static Log logger = LogFactory.getLog(RomeFeedView.class);

    /**
     * 
     * @see org.springframework.web.servlet.View#render(java.util.Map,
     *      javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void render(Map model, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        SyndFeed feed = (SyndFeed) model.get("romeFeed");
        
        resp.setContentType("text/xml; charset=" + feed.getEncoding());
        PrintWriter writer = resp.getWriter();

        try {
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
            writer.close();
        } catch (IOException e) {
            logger.error("IO exception for RSS build", e);
        } catch (FeedException e) {
            logger.error("Error when constructing RSS feed", e);
        } catch (Exception e) {
            logger.error("RSS error: ", e);
        }
    } // end of render()

 
    public void afterPropertiesSet() throws Exception {
        //doNothing
    }

    public ReferenceDataProvider[] getReferenceDataProviders() {
        return referenceDataProviders;
    }

    public void setReferenceDataProviders(ReferenceDataProvider[] referenceDataProviders) {
        this.referenceDataProviders = referenceDataProviders;
    }

}
