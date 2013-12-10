/* Copyright (c) 2010, 2013, University of Oslo, Norway
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
package org.vortikal.web.display.linkcheck;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.web.JSONController;
import org.vortikal.web.RequestContext;
import org.vortikal.web.display.linkcheck.LinkChecker.LinkCheckResult;
import org.vortikal.web.service.URL;

public class LinkCheckController extends JSONController {

    private LinkChecker linkChecker;
    
    private final static String LINK = "link";
    private final static String STATUS = "status";
    private final static String MSG = "msg";
    
    private final class LinkTypes {
        public final static String ANCHOR = "#";
        public final static String MAIL_TO = "mailto:";
        public final static String FTP = "ftp:";
        public final static String JAVASCRIPT = "javascript:";
        public final static String FILE = "file:";
        public final static String WEBCAL = "webcal:";
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> urls;
        try {
            urls = readInput(request);
        } catch (BadRequestException e) {
            badRequest(e, response);
            return null;
        }
        URL base = URL.create(request);
        base.clearParameters();
        List<LinkCheckResult> results = checkLinks(urls, base, shouldSendReferrer());
        writeResults(results, response);
        return null;
    }

    private List<LinkCheckResult> checkLinks(List<String> input, URL base, boolean sendReferrer) {
        List<LinkCheckResult> results = new ArrayList<LinkCheckResult>();
        for (String link : input) {
            LinkCheckResult r = this.linkChecker.validate(link, base, sendReferrer);
            results.add(r);
        }
        return results;
    }

    private void writeResults(List<LinkCheckResult> results, HttpServletResponse response) throws Exception {
        JSONArray list = new JSONArray();
        for (LinkCheckResult result : results) {
            JSONObject o = new JSONObject();
            o.put(LINK, result.getLink());
            o.put(STATUS, result.getStatus().toString());
            if (result.getReason() != null) {
                o.put(MSG, result.getReason());
            }
            list.add(o);
        }
        okRequest(list, response);
    }

    private List<String> readInput(HttpServletRequest request) throws Exception {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.startsWith("text/plain")) {
            throw new BadRequestException("Request data not textual");
        }
        BufferedReader reader = request.getReader();
        try {
            String line;
            int n = 0;
            List<String> urls = new ArrayList<String>();
            while ((line = reader.readLine()) != null) {
                if (line.length() > 500) {
                    continue; // Facebook API link often exceeds 500 chars -
                              // ignore it
                }
                if (++n > 10) {
                    throw new BadRequestException("Too many lines");
                }
                line = sanitize(line);
                if (line != null) {
                    urls.add(line);
                }
            }
            return urls;
        } finally {
            reader.close();
        }
    }

    private String sanitize(String input) {
        if (input == null) {
            return null;
        }
        if ("".equals(input.trim())) {
            return null;
        }
        if (input.startsWith(LinkTypes.ANCHOR) || input.startsWith(LinkTypes.MAIL_TO) || input.startsWith(LinkTypes.FTP)
                || input.startsWith(LinkTypes.JAVASCRIPT) || input.startsWith(LinkTypes.FILE) || input.startsWith(LinkTypes.WEBCAL)) {
            return null;
        }
        return input;
    }

    private boolean shouldSendReferrer() {
        try {
            RequestContext rc = RequestContext.getRequestContext();
            Repository repo = rc.getRepository();
            Resource r = repo.retrieve(rc.getSecurityToken(), rc.getResourceURI(), true);
            return !r.isReadRestricted();
        } catch (Exception e) {
            return false;
        }
    }

    @Required
    public void setLinkChecker(LinkChecker linkChecker) {
        this.linkChecker = linkChecker;
    }

}
