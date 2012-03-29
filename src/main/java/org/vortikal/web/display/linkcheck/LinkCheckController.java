/* Copyright (c) 2010, University of Oslo, Norway
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.web.display.linkcheck.LinkChecker.LinkCheckResult;
import org.vortikal.web.service.URL;

public class LinkCheckController implements Controller, InitializingBean {

    private LinkChecker linkChecker;

    @Override
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> urls = null;
        try {
            urls = readInput(request);
        } catch (BadRequestException e) {
            badRequest(e, response);
            return null;
        }
        URL base = URL.create(request);
        base.clearParameters();
        List<LinkCheckResult> results = checkLinks(urls, base);
        writeResults(results, response);
        return null;
    }
    
    private List<LinkCheckResult> checkLinks(List<String> input, URL base) {
        List<LinkCheckResult> results = new ArrayList<LinkCheckResult>();
        for (String link: input) {
            LinkCheckResult r = this.linkChecker.validate(link, base);
            results.add(r);
        }
        return results;
    }

    private void badRequest(Throwable e, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        PrintWriter writer = response.getWriter();
        try {
            writer.write(e.getMessage());
        } finally {
            writer.close();
        }
    }
    
    private void writeResults(List<LinkCheckResult> results, HttpServletResponse response) throws Exception {
        JSONArray list = new JSONArray();
        for (LinkCheckResult result: results) {
            JSONObject o = new JSONObject();
            o.put("link", result.getLink());
            o.put("status", result.getStatus().toString());
            if (result.getReason() != null) {
                o.put("message", result.getReason());
            }
            list.add(o);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain;charset=utf-8");
        PrintWriter writer = response.getWriter();
        try {
            writer.print(list.toString(1));
        } finally {
            writer.close();
        }
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
                    throw new BadRequestException("Line too long: " + line.length());
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
        if (input.startsWith("#") 
         || input.startsWith("mailto:") 
         || input.startsWith("ftp:")
         || input.startsWith("javascript:")
         || input.startsWith("file:")) {
            return null;
        }
        return input;
    }

    private static class BadRequestException extends Exception {
        private static final long serialVersionUID = -8967067839019333139L;
        public BadRequestException(String msg) {
            super(msg);
        }
    }
    
    @Required
    public void setLinkChecker(LinkChecker linkChecker) {
        this.linkChecker = linkChecker;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
