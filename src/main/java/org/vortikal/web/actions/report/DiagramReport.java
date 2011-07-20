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
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;

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
    private SearchComponent wordSearch;
    private SearchComponent pptSearch;
    private SearchComponent excelSearch;

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

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

            l = this.imageSearch.execute(request, resource, 1, 1, 0);
            int image = l.getTotalHits();
            result.put("image", image);

            l = this.audioSearch.execute(request, resource, 1, 1, 0);
            int audio = l.getTotalHits();
            result.put("audio", audio);

            l = this.videoSearch.execute(request, resource, 1, 1, 0);
            int video = l.getTotalHits();
            result.put("video", video);

            l = this.pdfSearch.execute(request, resource, 1, 1, 0);
            int pdf = l.getTotalHits();
            result.put("pdf", pdf);

            l = this.wordSearch.execute(request, resource, 1, 1, 0);
            int word = l.getTotalHits();
            result.put("word", word);

            l = this.pptSearch.execute(request, resource, 1, 1, 0);
            int ppt = l.getTotalHits();
            result.put("ppt", ppt);

            l = this.excelSearch.execute(request, resource, 1, 1, 0);
            int excel = l.getTotalHits();
            result.put("excel", excel);

            result.put("secondtotal", webpage + image + audio + video + pdf + word + ppt + excel);
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
    public void setWordSearch(SearchComponent wordSearch) {
        this.wordSearch = wordSearch;
    }

    @Required
    public void setPptSearch(SearchComponent pptSearch) {
        this.pptSearch = pptSearch;
    }

    @Required
    public void setExcelSearch(SearchComponent excelSearch) {
        this.excelSearch = excelSearch;
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
