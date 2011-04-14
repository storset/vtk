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
package org.vortikal.web.display.collection.event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.display.listing.ListingPager;
import org.vortikal.web.servlet.ResourceAwareLocaleResolver;

import com.ibm.icu.util.Calendar;

public final class EventListingHelper implements InitializingBean {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\d{4}-\\d{2}");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");
    private Map<Pattern, SimpleDateFormat> dateformats;
    private Map<Pattern, SpecificDateSearchType> searchTypes;

    private DateValueFormatter dateValueFormatter;
    private ResourceAwareLocaleResolver localeResolver;
    private PropertyTypeDefinition eventTypeTitlePropDef;

    public static final String REQUEST_PARAMETER_DATE = "date";
    public static final String REQUEST_PARAMETER_VIEW = "view";
    public static final String VIEW_TYPE_ALL_UPCOMING = "allupcoming";
    public static final String VIEW_TYPE_ALL_PREVIOUS = "allprevious";

    public enum SpecificDateSearchType {
        Day, Month, Year;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.dateformats = new HashMap<Pattern, SimpleDateFormat>();
        this.dateformats.put(DATE_PATTERN, new SimpleDateFormat("yyyy-MM-dd"));
        this.dateformats.put(MONTH_PATTERN, new SimpleDateFormat("yyyy-MM"));
        this.dateformats.put(YEAR_PATTERN, new SimpleDateFormat("yyyy"));
        this.searchTypes = new HashMap<Pattern, SpecificDateSearchType>();
        this.searchTypes.put(DATE_PATTERN, SpecificDateSearchType.Day);
        this.searchTypes.put(MONTH_PATTERN, SpecificDateSearchType.Month);
        this.searchTypes.put(YEAR_PATTERN, SpecificDateSearchType.Year);
    }

    public SpecificDateSearchType getSpecificDateSearchType(HttpServletRequest request) {
        String specificDate = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
        if (specificDate != null && !"".equals(specificDate.trim())) {
            for (Pattern regex : this.searchTypes.keySet()) {
                if (regex.matcher(specificDate).matches()) {
                    return this.searchTypes.get(regex);
                }
            }
        }
        return null;
    }

    public Date getSpecificSearchDate(HttpServletRequest request) {
        String specificDate = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
        if (specificDate != null && !"".equals(specificDate.trim())) {
            SimpleDateFormat sdf = null;
            for (Pattern regex : this.searchTypes.keySet()) {
                if (regex.matcher(specificDate).matches()) {
                    sdf = this.dateformats.get(regex);
                }
            }
            if (sdf != null) {
                try {
                    Date date = sdf.parse(specificDate);
                    return date;
                } catch (ParseException pee) { // Hehe.. pee..
                    // Ignore, return null
                }
            }
        }
        return null;
    }

    public String getEventTypeTitle(HttpServletRequest request, Resource collection, String key, boolean capitalize) {
        return this.getEventTypeTitle(request, collection, null, null, key, capitalize, true);
    }

    public String getEventTypeTitle(HttpServletRequest request, Resource collection, SpecificDateSearchType searchType,
            Date date, String key, boolean capitalize, boolean includePage) {
        List<Object> params = new ArrayList<Object>();
        String eventTypeTitle = this.getEventTypeTitle(collection, capitalize);
        if (eventTypeTitle != null) {
            key = key + ".overrideDefault";
            params.add(eventTypeTitle);
        }
        if (searchType != null && date != null) {
            String titleDate = this.getRequestedDateAsLocalizedString(collection, searchType, date);
            params.add(titleDate);
        }
        String title = getLocalizedTitle(request, key, params.toArray());
        int page = ListingPager.getPage(request, ListingPager.UPCOMING_PAGE_PARAM);
        if (includePage && page > 1) {
            String pageText = this.getLocalizedTitle(request, "viewCollectionListing.page", null);
            title = title + " - " + pageText + " " + page;
        }
        return title;
    }

    public String getLocalizedTitle(HttpServletRequest request, String key, Object[] params) {
        RequestContext springRequestContext = new RequestContext(request);
        if (params != null) {
            return springRequestContext.getMessage(key, params);
        }
        return springRequestContext.getMessage(key);
    }

    public String getEventTypeTitle(Resource collection, boolean capitalize) {
        Property eventTypeTitleProp = collection.getProperty(this.eventTypeTitlePropDef);
        if (eventTypeTitleProp != null) {
            String eventTypeTitle = eventTypeTitleProp.getStringValue();
            eventTypeTitle = capitalize ? eventTypeTitle.substring(0, 1).toUpperCase()
                    + eventTypeTitle.substring(1).toLowerCase() : eventTypeTitle.toLowerCase();
            return eventTypeTitle;
        }
        return null;
    }

    private String getRequestedDateAsLocalizedString(Resource collection,
            SpecificDateSearchType searchType, Date date) {
        Calendar requestedCal = Calendar.getInstance();
        requestedCal.setTime(date);
        if (searchType != SpecificDateSearchType.Year) {
            Locale locale = this.localeResolver.resolveResourceLocale(collection.getURI());
            String format = "full-month-year-short";
            if (searchType == SpecificDateSearchType.Month) {
                format = "full-month-year";
            }
            return this.dateValueFormatter.valueToString(new Value(date, false), format, locale);
        }
        return String.valueOf(requestedCal.get(Calendar.YEAR));
    }

    @Required
    public void setDateValueFormatter(DateValueFormatter dateValueFormatter) {
        this.dateValueFormatter = dateValueFormatter;
    }

    @Required
    public void setLocaleResolver(ResourceAwareLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Required
    public void setEventTypeTitlePropDef(PropertyTypeDefinition eventTypeTitlePropDef) {
        this.eventTypeTitlePropDef = eventTypeTitlePropDef;
    }

}
