package org.vortikal.web.controller.ical;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.HtmlValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;

public class ICalController implements Controller {
	
	private Repository repository;

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String token = SecurityContext.getSecurityContext().getToken();
        Path uri = RequestContext.getRequestContext().getResourceURI();

        Resource event = this.repository.retrieve(token, uri, true);

        String iCal = createICal(event);
        if (iCal == null) {
        	return null;
        }
        
		response.setContentType("text/calendar;charset=utf-8");
		String iCalfileName = getICalFileName(event);
		response.setHeader("Content-Disposition","filename=" + iCalfileName + ".ics");
		ServletOutputStream out = response.getOutputStream();
		out.print(iCal);
		out.close();
		
		return null;
	}

	private String getICalFileName(Resource event) {
		String resourceUri = event.getURI().toString();
		return resourceUri.substring(resourceUri.lastIndexOf("/") + 1, resourceUri.lastIndexOf("."));
	}

	private String createICal(Resource event) {
		Property startDate = event.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.START_DATE_PROP_NAME);
		// We don't create anything unless we have the startdate
		if (startDate == null) {
			return null;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("BEGIN:VCALENDAR\n");
		sb.append("VERSION:2.0\n");
		sb.append("BEGIN:VEVENT\n");
		sb.append("DTSTART:" + getICalDate(startDate) + "\n");
		
		Property endDate = event.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.END_DATE_PROP_NAME);
		if (endDate != null) {
			sb.append("DTEND:" + getICalDate(endDate) + "\n");
		}
		
		Property location = event.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.LOCATION_PROP_NAME);
		if (location != null) {
			sb.append("LOCATION:" + location.getStringValue() + "\n");
		}
		
		Property description = event.getProperty(Namespace.DEFAULT_NAMESPACE, PropertyType.INTRODUCTION_PROP_NAME);
		if (description != null && StringUtils.isNotBlank(description.getStringValue())) {
			String flattenedDescription = description.getFormattedValue(HtmlValueFormatter.FLATTENED_FORMAT, null);
			sb.append("DESCRIPTION:" + flattenedDescription + "\n");
		}

		sb.append("SUMMARY:" + event.getTitle() + "\n");
		sb.append("END:VEVENT\n");
		sb.append("END:VCALENDAR");
		return sb.toString();
	}

	private String getICalDate(Property date) {
		Date eventDate = date.getDateValue();
		// Local time
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat timeFormat = new SimpleDateFormat("hhmmss");
		return dateFormat.format(eventDate) + "T" + timeFormat.format(eventDate);
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

}
