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
package org.vortikal.web.display.listing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.vortikal.repository.search.Search;
import org.vortikal.web.service.URL;

public class ListingPager {

    public static final String UPCOMING_PAGE_PARAM = "page";
    public static final String PREVIOUS_PAGE_PARAM = "p-page";
    public static final String PREV_BASE_OFFSET_PARAM = "p-offset";
    public static final String USER_DISPLAY_PAGE = "u-page";

    public static List<URL> generatePageThroughUrls(int hits, int pageLimit, URL baseURL) {
        return generatePageThroughUrls(hits, pageLimit, 0, baseURL, false);
    }

    public static List<URL> generatePageThroughUrls(int hits, int pageLimit, int hitsInFirstSearch, URL baseURL,
            boolean twoSearches) {
        if (pageLimit == 0) {
            return null;
        }
        List<URL> urls = new ArrayList<URL>();
        baseURL = new URL(baseURL)
            .removeParameter(PREVIOUS_PAGE_PARAM)
            .removeParameter(PREV_BASE_OFFSET_PARAM)
            .removeParameter(UPCOMING_PAGE_PARAM)
            .removeParameter(USER_DISPLAY_PAGE)
            .setCollection(true);
        
        int maxPages = Search.MAX_LIMIT / pageLimit;
        
        int pages = hits / pageLimit;
        if ((hits % pageLimit) > 0) {
            pages += 1;
        }
        if (pages >= maxPages) {
            pages = maxPages;
        }
        int pagesInFirstSearch = (hitsInFirstSearch / pageLimit);
        if ((hitsInFirstSearch % pageLimit) > 0) {
            pagesInFirstSearch += 1;
        }
        int offset = 0;
        if (hitsInFirstSearch > 0 && (hitsInFirstSearch % pageLimit) > 0) {
            offset = pageLimit - (hitsInFirstSearch % pageLimit);
        }

        int j = 1;
        for (int i = 0; i < pages; i++) {
            URL url = new URL(baseURL);
            if (i == 0) {
                urls.add(url);
                continue;
            }
            if (hitsInFirstSearch == 0 && twoSearches) {
                url.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(i + 1));
            } else if (pagesInFirstSearch > i || hitsInFirstSearch == 0) {
                url.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(i + 1));
            } else {
                if (pagesInFirstSearch > 1) {
                    url.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(pagesInFirstSearch));
                }
                if (offset > 0) {
                    url.setParameter(PREV_BASE_OFFSET_PARAM, String.valueOf(offset));
                }
                url.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(j));
                j++;
            }
            url.setParameter(USER_DISPLAY_PAGE, String.valueOf(i + 1));
            urls.add(url);
        }

        return urls;
    }
    
    
    public static int getPage(HttpServletRequest request, String parameter) {
        int page = 1;
        String pageParam = request.getParameter(parameter);
        if (StringUtils.isNotBlank(pageParam)) {
            try {
                page = Integer.parseInt(pageParam);
                if (page < 1) {
                    page = 1;
                }
            } catch (Throwable t) {
            }
        }
        return page;
    }
}
