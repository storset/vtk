package org.vortikal.web.controller;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.abdera.Abdera;
import org.apache.commons.lang.StringUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceTypeTreeImpl;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.controller.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CollectionListingAsAtomFeedTest extends TestCase {
	
	private CollectionListingAsAtomFeed controller;
	
	private Path requesPath;
	
	private Mockery context = new JUnit4Mockery();
	private final HttpServletRequest mockRequest = context.mock(HttpServletRequest.class);
	private final HttpServletResponse mockResponse = context.mock(HttpServletResponse.class);
	private final Repository mockRepository = context.mock(Repository.class);
	private final Service mockViewService = context.mock(Service.class);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		requesPath = Path.fromString("/atomfeedtest");
		
        BaseContext.pushContext();
        RequestContext requestContext = new RequestContext(null, null, requesPath);
        RequestContext.setRequestContext(requestContext);
        SecurityContext securityContext = new SecurityContext(null, null);
        SecurityContext.setSecurityContext(securityContext);
		
		controller = new CollectionListingAsAtomFeed();
		controller.setAbdera(new Abdera());
		controller.setRepository(mockRepository);
		controller.setViewService(mockViewService);
		controller.setSearchComponent(new MockSearchComponent());
	}
	
	public void testCreateFeed() throws Exception {
		
		// Retrieve the collection to create feed from
		context.checking(new Expectations() {{ one(mockRepository).retrieve(null, requesPath, true); will(returnValue(getCollection())); }});
		
		// Set main feed id
		final URL url = new URL("http", "localhost", requesPath);
		context.checking(new Expectations() {{ one(mockViewService).constructURL(requesPath); will(returnValue(url)); }});
		
		final String link = requesPath.toString();
		context.checking(new Expectations() {{ one(mockViewService).constructLink(requesPath); will(returnValue(link)); }});
		
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
