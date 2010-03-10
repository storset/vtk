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
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.support.RequestContext;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.web.servlet.ResourceAwareLocaleResolver;

import com.ibm.icu.util.Calendar;

public final class EventListingHelper {

    private DateValueFormatter dateValueFormatter;
    private ResourceAwareLocaleResolver localeResolver;

    public static final String REQUEST_PARAMETER_DATE = "date";
    public static final String REQUEST_PARAMETER_VIEW = "view";
    public static final String VIEW_TYPE_ALL_UPCOMING = "allupcoming";
    public static final String VIEW_TYPE_ALL_PREVIOUS = "allprevious";

    public enum SpecificDateSearchType {
        Day, Month, Year;
    }

    public SpecificDateSearchType getSpecificDateSearchType(HttpServletRequest request) {
        String specificDate = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
        if (specificDate != null && !"".equals(specificDate.trim())) {
            if (specificDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return SpecificDateSearchType.Day;
            } else if (specificDate.matches("\\d{4}-\\d{2}")) {
                return SpecificDateSearchType.Month;
            } else if (specificDate.matches("\\d{4}")) {
                return SpecificDateSearchType.Year;
            }
        }
        return null;
    }

    public Date getSpecificSearchDate(HttpServletRequest request) {
        String specificDate = request.getParameter(EventListingHelper.REQUEST_PARAMETER_DATE);
        if (specificDate != null && !"".equals(specificDate.trim())) {
            SimpleDateFormat sdf = null;
            if (specificDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                sdf = new SimpleDateFormat("yyyy-MM-dd");
            } else if (specificDate.matches("\\d{4}-\\d{2}")) {
                sdf = new SimpleDateFormat("yyyy-MM");
            } else if (specificDate.matches("\\d{4}")) {
                sdf = new SimpleDateFormat("yyyy");
            }
            try {
                Date date = sdf.parse(specificDate);
                return date;
            } catch (ParseException pee) { // Hehe.. pee..
                // Ignore, return null
            }
        }
        return null;
    }

    public String getRequestedDateAsLocalizedString(HttpServletRequest request, Resource collection,
            SpecificDateSearchType searchType, Date date) {
        Calendar requestedCal = Calendar.getInstance();
        requestedCal.setTime(date);
        if (searchType != SpecificDateSearchType.Year) {
            Locale locale = this.localeResolver.resolveResourceLocale(request, collection.getURI());
            String format = "short";
            if (searchType == SpecificDateSearchType.Month) {
                format = "month-year";
            }
            return this.dateValueFormatter.valueToString(new Value(date, false), format, locale);
        }
        return String.valueOf(requestedCal.get(Calendar.YEAR));
    }

    public String getTitle(HttpServletRequest request, String key, Object[] params) {
        RequestContext springRequestContext = new RequestContext(request);
        if (params != null) {
            return springRequestContext.getMessage(key, params);
        }
        return springRequestContext.getMessage(key);
    }

    @Required
    public void setDateValueFormatter(DateValueFormatter dateValueFormatter) {
        this.dateValueFormatter = dateValueFormatter;
    }

    @Required
    public void setLocaleResolver(ResourceAwareLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }
}
