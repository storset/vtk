/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.display.ical;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

    

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.net.NetUtils;
import org.vortikal.web.RequestContext;
import org.joda.time.DateTimeZone;

public class ICalController implements Controller {

    private Repository repository;
    private PropertyTypeDefinition startDatePropDef;
    private PropertyTypeDefinition endDatePropDef;
    private PropertyTypeDefinition locationPropDef;    
    
    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource event = this.repository.retrieve(token, uri, true);

        String iCal = createICal(event);
        if (iCal == null) {
            return null;
        }

        response.setContentType("text/calendar;charset=utf-8");
        String iCalfileName = getICalFileName(event);
        response.setHeader("Content-Disposition", "filename=" + iCalfileName + ".ics");
        ServletOutputStream out = response.getOutputStream();
        out.print(iCal);
        out.close();

        return null;
    }


    private String getICalFileName(Resource event) {
        String name = event.getName();
        if (name.contains(".")) {
            name = name.substring(0, name.indexOf("."));
        }
        return name;
    }


    private String createICal(Resource event) {
        
        //Spec: http://www.ietf.org/rfc/rfc2445.txt
        // PRODID (4.7.3) & UID (4.8.4.7) added as recommended by spec. DTEND is not required.
        // If DTEND not present, DTSTART will count for both start & end, as stated in spec (4.6.1).
        
        Property startDate = event.getProperty(startDatePropDef);
        // We don't create anything unless we have the startdate
        if (startDate == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\n");
        sb.append("VERSION:2.0\n");
        sb.append("METHOD:PUBLISH\n");
        sb.append("PRODID:-//UiO//Vortikal//NONSGML v1.0//NO\n");
        sb.append("BEGIN:VEVENT\n");
        sb.append("DTSTAMP:" + getDtstamp() + "\n");
        sb.append("UID:" + getUiD(Calendar.getInstance().getTime()) + "\n");
        sb.append("DTSTART:" + getICalDate(startDate.getDateValue()) + "Z\n");
        
        Property endDate = event.getProperty(endDatePropDef);
        if (endDate != null) {
            sb.append("DTEND:" + getICalDate(endDate.getDateValue()) + "Z\n");
        }

        Property location = event.getProperty(locationPropDef);
        if (location != null) {
            sb.append("LOCATION:" + location.getStringValue() + "\n");
        }

        Property description = event.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.INTRODUCTION_PROP_NAME);
        if (description != null && StringUtils.isNotBlank(description.getStringValue())) {
            sb.append("DESCRIPTION:" + getDescription(description) + "\n");
        }

        sb.append("SUMMARY:" + event.getTitle() + "\n");
        sb.append("END:VEVENT\n");
        sb.append("END:VCALENDAR");
        return sb.toString();
    }


    private String getDtstamp() {
        String dateFormat = "yyyyMMdd'T'HHmmss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(Calendar.getInstance().getTime());
    }


    private String getUiD(Date currenttime) {
        return getICalDate(currenttime) + "-" + Calendar.getInstance().getTimeInMillis() + "@" + NetUtils.guessHostName();
    }


    private String getICalDate(Date date) {  
        DateTimeZone zone = DateTimeZone.getDefault();
        Date UTCDate = new Date(zone.convertLocalToUTC(date.getTime(), true));
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
        return dateFormat.format(UTCDate) + "T" + timeFormat.format(UTCDate);
    }


    private String getDescription(Property description) {
        String flattenedDescription = description.getFormattedValue(HtmlValueFormatter.FLATTENED_FORMAT, null);
        // Remove linebreaks and the like...
        flattenedDescription = flattenedDescription.replaceAll("(\r\n|\r|\n|\n\r|\t)", " ");
        // Remove multiple whitespaces between words
        flattenedDescription = flattenedDescription.replaceAll("\\b\\s{2,}\\b", " ");
        return flattenedDescription;
    }


    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    
    @Required
    public void setStartDatePropDef(PropertyTypeDefinition startDatePropDef) {
        this.startDatePropDef = startDatePropDef;
    }
    
    
    @Required
    public void setEndDatePropDef(PropertyTypeDefinition endDatePropDef) {
        this.endDatePropDef = endDatePropDef;
    }
    
    
    @Required
    public void setLocationPropDef(PropertyTypeDefinition locationPropDef) {
        this.locationPropDef = locationPropDef;
    }

}
