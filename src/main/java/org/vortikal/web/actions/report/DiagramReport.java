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
package org.vortikal.web.actions.report;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class DiagramReport implements Reporter {

    private String name;
    private String viewName;
    private SearchComponent filesSearch;
    private SearchComponent folderSearch;
    private SearchComponent webpageSearch;
    private SearchComponent imageSearch;
    private SearchComponent audioSearch;
    private SearchComponent videoSearch;
    private SearchComponent pdfSearch;
    private SearchComponent docSearch;
    private SearchComponent pptSearch;
    private SearchComponent xlsSearch;
    
    private static final String REPORT_TYPE_PARAM = "report-type";

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();

        URL backURL = new URL(service.constructURL(resource, securityContext.getPrincipal()));

        Listing l;
        try {
            l = this.filesSearch.execute(request, resource, 1, 1, 0);
            int files = l.getTotalHits();
            result.put("files", files);

            l = this.folderSearch.execute(request, resource, 1, 1, 0);
            int folders = l.getTotalHits();
            result.put("folders", folders);

            result.put("firsttotal", files + folders);
        } catch (Exception e) {
        }

        try {
            l = this.webpageSearch.execute(request, resource, 1, 1, 0);
            int webpage = l.getTotalHits();
            result.put("webpage", webpage);
            URL webpageURL = new URL(backURL);
            webpageURL.addParameter(REPORT_TYPE_PARAM, "webpageReporter");
            result.put("webpageURL", webpageURL);

            l = this.imageSearch.execute(request, resource, 1, 1, 0);
            int image = l.getTotalHits();
            result.put("image", image);
            URL imageURL = new URL(backURL);
            imageURL.addParameter(REPORT_TYPE_PARAM, "imageReporter");
            result.put("imageURL", imageURL);

            l = this.audioSearch.execute(request, resource, 1, 1, 0);
            int audio = l.getTotalHits();
            result.put("audio", audio);
            URL audioURL = new URL(backURL);
            audioURL.addParameter(REPORT_TYPE_PARAM, "audioReporter");
            result.put("audioURL", audioURL);

            l = this.videoSearch.execute(request, resource, 1, 1, 0);
            int video = l.getTotalHits();
            result.put("video", video);
            URL videoURL = new URL(backURL);
            videoURL.addParameter(REPORT_TYPE_PARAM, "videoReporter");
            result.put("videoURL", videoURL);

            l = this.pdfSearch.execute(request, resource, 1, 1, 0);
            int pdf = l.getTotalHits();
            result.put("pdf", pdf);
            URL pdfURL = new URL(backURL);
            pdfURL.addParameter(REPORT_TYPE_PARAM, "pdfReporter");
            result.put("pdfURL", pdfURL);

            l = this.docSearch.execute(request, resource, 1, 1, 0);
            int doc = l.getTotalHits();
            result.put("doc", doc);
            URL docURL = new URL(backURL);
            docURL.addParameter(REPORT_TYPE_PARAM, "docReporter");
            result.put("docURL", docURL);

            l = this.pptSearch.execute(request, resource, 1, 1, 0);
            int ppt = l.getTotalHits();
            result.put("ppt", ppt);
            URL pptURL = new URL(backURL);
            pptURL.addParameter(REPORT_TYPE_PARAM, "pptReporter");
            result.put("pptURL", pptURL);

            l = this.xlsSearch.execute(request, resource, 1, 1, 0);
            int xls = l.getTotalHits();
            result.put("xls", xls);
            URL xlsURL = new URL(backURL);
            xlsURL.addParameter(REPORT_TYPE_PARAM, "xlsReporter");
            result.put("xlsURL", xlsURL);

            result.put("secondtotal", webpage + image + audio + video + pdf + doc + ppt + xls);
        } catch (Exception e) {
        }

        return result;
    }

    @Required
    public void setFilesSearch(SearchComponent filesSearch) {
        this.filesSearch = filesSearch;
    }

    @Required
    public void setFolderSearch(SearchComponent folderSearch) {
        this.folderSearch = folderSearch;
    }

    @Required
    public void setWebpageSearch(SearchComponent webpageSearch) {
        this.webpageSearch = webpageSearch;
    }

    @Required
    public void setImageSearch(SearchComponent imageSearch) {
        this.imageSearch = imageSearch;
    }

    @Required
    public void setAudioSearch(SearchComponent audioSearch) {
        this.audioSearch = audioSearch;
    }

    @Required
    public void setVideoSearch(SearchComponent videoSearch) {
        this.videoSearch = videoSearch;
    }

    @Required
    public void setPdfSearch(SearchComponent pdfSearch) {
        this.pdfSearch = pdfSearch;
    }

    @Required
    public void setDocSearch(SearchComponent docSearch) {
        this.docSearch = docSearch;
    }

    @Required
    public void setPptSearch(SearchComponent pptSearch) {
        this.pptSearch = pptSearch;
    }

    @Required
    public void setXlsSearch(SearchComponent xlsSearch) {
        this.xlsSearch = xlsSearch;
    }

    @Override
    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
}
