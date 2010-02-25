package org.vortikal.web.display.listing;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.vortikal.web.service.URL;

public class ListingPager {

    public static final String UPCOMING_PAGE_PARAM = "page";
    public static final String PREVIOUS_PAGE_PARAM = "p-page";
    public static final String PREV_BASE_OFFSET_PARAM = "p-offset";
    public static final String USER_DISPLAY_PAGE = "u-page";

    public static List<URL> generatePageThroughUrls(int hits, int pageLimit, URL baseURL) {
        return generatePageThroughUrls(hits, pageLimit, 0, baseURL, false);
    }

    public static List<URL> generatePageThroughUrls(int hits, int pageLimit, int hitsReturnedByFirstSearch, URL baseURL,
            boolean twoSearches) {
        if (pageLimit == 0) {
            return null;
        }
        List<URL> urls = new ArrayList<URL>();

        baseURL.removeParameter(PREVIOUS_PAGE_PARAM);
        baseURL.removeParameter(PREV_BASE_OFFSET_PARAM);
        baseURL.removeParameter(UPCOMING_PAGE_PARAM);
        baseURL.removeParameter(USER_DISPLAY_PAGE);

        int pages = hits / pageLimit;
        if ((hits % pageLimit) > 0) {
            pages += 1;
        }
        int pagesUsedToDisplayResultsOfTheFirstSearch = (hitsReturnedByFirstSearch / pageLimit);
        if ((hitsReturnedByFirstSearch % pageLimit) > 0) {
            pagesUsedToDisplayResultsOfTheFirstSearch += 1;
        }
        int offset = 0;
        if (hitsReturnedByFirstSearch > 0 && (hitsReturnedByFirstSearch % pageLimit) > 0) {
            offset = pageLimit - (hitsReturnedByFirstSearch % pageLimit);
        }

        int j = 1;
        for (int i = 0; i < pages; i++) {
            URL url = URL.create(baseURL);
            if (i == 0) {
                urls.add(url);
                continue;
            }
            if (hitsReturnedByFirstSearch == 0 && twoSearches) {
                url.setParameter(PREVIOUS_PAGE_PARAM, String.valueOf(i + 1));
            } else if (pagesUsedToDisplayResultsOfTheFirstSearch > i || hitsReturnedByFirstSearch == 0) {
                url.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(i + 1));
            } else {
                if (pagesUsedToDisplayResultsOfTheFirstSearch > 1) {
                    url.setParameter(UPCOMING_PAGE_PARAM, String.valueOf(pagesUsedToDisplayResultsOfTheFirstSearch));
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
        int page = 0;
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

        if (page == 0) {
            page = 1;
        }
        return page;
    }

    public static URL getBaseURL(HttpServletRequest request) {
        return URL.parse(URL.create(request).getBase());
    }

}
