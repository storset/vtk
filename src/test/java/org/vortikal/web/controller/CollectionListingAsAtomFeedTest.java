package org.vortikal.web.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.Abdera;
import org.apache.commons.lang.StringUtils;
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
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CollectionListingAsAtomFeedTest extends AbstractControllerTest {
	
	private CollectionListingAsAtomFeed controller;
	
	private Path requestPath;
	private final Service mockViewService = context.mock(Service.class);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		controller = new CollectionListingAsAtomFeed();
		controller.setAbdera(new Abdera());
		controller.setRepository(mockRepository);
		controller.setViewService(mockViewService);
		controller.setSearchComponent(new MockSearchComponent());
	}
	
	public Path getRequestPath() {
		requestPath = Path.fromString("/atomfeedtest");
		return requestPath;
	}
	
	public void testCreateFeed() throws Exception {
		
		// Retrieve the collection to create feed from
		context.checking(new Expectations() {{ one(mockRepository).retrieve(null, requestPath, true); will(returnValue(getCollection())); }});
		
		// Set main feed id
		final URL url = new URL("http", "localhost", requestPath);
		context.checking(new Expectations() {{ one(mockViewService).constructURL(requestPath); will(returnValue(url)); }});
		
		final String link = requestPath.toString();
		context.checking(new Expectations() {{ one(mockViewService).constructLink(requestPath); will(returnValue(link)); }});
		
		// Set the contenttype on the response
		context.checking(new Expectations() {{ one(mockResponse).setContentType("application/atom+xml;charset=utf-8"); }});
		
		// Set the writer to use on response
		StringWriter out = new StringWriter();
		final PrintWriter writer = new PrintWriter(out);
		context.checking(new Expectations() {{ one(mockResponse).getWriter(); will(returnValue(writer)); }});
		
		ModelAndView result = controller.handleRequest(mockRequest, mockResponse);
		
		assertNull("An unexpected model&view was returned", result);
		
		String feed = out.toString();
		assertTrue("Feed is empty", StringUtils.isNotBlank(feed));
		
		// TODO validate feedxml
		
	}
	
	private Resource getCollection() {
		ResourceImpl collection = new ResourceImpl();
		
        PropertyTypeDefinitionImpl propDef = new PropertyTypeDefinitionImpl();
        propDef.setValueFormatter(new DateValueFormatter());
        propDef.setType(Type.DATE);
        propDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
        propDef.setName(PropertyType.CREATIONTIME_PROP_NAME);
        Property publishedProp = propDef.createProperty(Calendar.getInstance().getTime());
        
        // TODO set other properties needed for proper testing of feed
		
        collection.setResourceTypeTree(new ResourceTypeTreeImpl());
        collection.addProperty(publishedProp);
        
		return collection;
	}
	
	private class MockSearchComponent extends SearchComponent {
		
		@Override
		public Listing execute(HttpServletRequest request, Resource collection,
				int page, int pageLimit, int baseOffset) throws Exception {
			
			// TODO create testresources to generate feedentries from
			
			return new Listing(null, null, null, 0);
		}
		
	}

}
