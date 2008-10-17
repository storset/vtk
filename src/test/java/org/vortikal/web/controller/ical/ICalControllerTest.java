package org.vortikal.web.controller.ical;

import java.io.IOException;
import java.util.Calendar;

import javax.servlet.ServletOutputStream;

import org.jmock.Expectations;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceTypeTreeImpl;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.web.controller.AbstractControllerTest;

public class ICalControllerTest extends AbstractControllerTest {
	
	private ICalController controller;
	
	private Path requestPath;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
        controller = new ICalController();
        controller.setRepository(mockRepository);
	}
	
	public Path getRequestPath() {
		requestPath = Path.fromString("/event.html?ical");
		return requestPath;
	}
	
	public void testCreateICal() throws Exception {
		
		// Retrieve the resource (event) to create ical for
		context.checking(new Expectations() {{ one(mockRepository).retrieve(null, requestPath, true); will(returnValue(getEvent())); }});
		
		// Set the contenttype and content-description on the response
		context.checking(new Expectations() {{ one(mockResponse).setContentType("text/calendar;charset=utf-8"); }});
		context.checking(new Expectations() {{ one(mockResponse).setHeader("Content-Disposition", "filename=event.ics"); }});
		
		// Set the out outputstream to use on response
		final ServletOutputStream out = new MockServletOutputStream();
		context.checking(new Expectations() {{ atLeast(2).of(mockResponse).getOutputStream(); will(returnValue(out)); }});
		
		ModelAndView result = controller.handleRequest(mockRequest, mockResponse);
		assertNull("An unexpected model&view was returned", result);
		
		// TODO test the outputstream
		
	}
	
	private Resource getEvent() {
		ResourceImpl event = new ResourceImpl();
		event.setUri(requestPath);
		
        PropertyTypeDefinitionImpl startDatePropDef = new PropertyTypeDefinitionImpl();
        startDatePropDef.setValueFormatter(new DateValueFormatter());
        startDatePropDef.setType(Type.DATE);
        startDatePropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        startDatePropDef.setName(PropertyType.START_DATE_PROP_NAME);
        Property publishedProp = startDatePropDef.createProperty(Calendar.getInstance().getTime());
        
        // TODO set other properties required to test properly
		
        event.setResourceTypeTree(new ResourceTypeTreeImpl());
        event.addProperty(publishedProp);
        
		return event;
	}
	
	private class MockServletOutputStream extends ServletOutputStream {

		@Override
		public void write(int b) throws IOException {
		}
		
	}

}
