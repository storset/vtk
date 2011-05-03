package org.vortikal.web.display.collection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.abdera.Abdera;
import org.apache.commons.lang.StringUtils;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jmock.Expectations;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertySet;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.DateValueFormatter;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.StringValueFormatter;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalImpl;
import org.vortikal.security.SecurityContext;
import org.vortikal.testing.mocktypes.MockResourceTypeTree;
import org.vortikal.web.AbstractControllerTest;
import org.vortikal.web.RequestContext;
import org.vortikal.web.search.Listing;
import org.vortikal.web.search.SearchComponent;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.URL;

public class CollectionListingAsAtomFeedTest extends AbstractControllerTest {

    private CollectionListingAsAtomFeed controller;

    private Path requestPath;
    private final Service mockViewService = context.mock(Service.class);

    private String atomFeedRequestPath = "/atomfeedtest";
    private String host = "localhost";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new CollectionListingAsAtomFeed();
        controller.setAbdera(new Abdera());
        controller.setViewService(mockViewService);
        controller.setSearchComponent(new MockSearchComponent());
        controller.setCreationTimePropDef(getPropDef(Namespace.DEFAULT_NAMESPACE,
                PropertyType.CREATIONTIME_PROP_NAME, Type.HTML, new DateValueFormatter()));
        controller.setTitlePropDef(getPropDef(Namespace.DEFAULT_NAMESPACE,
                PropertyType.TITLE_PROP_NAME, Type.STRING, new StringValueFormatter()));
        controller.setLastModifiedPropDef(getPropDef(Namespace.DEFAULT_NAMESPACE,
                PropertyType.LASTMODIFIED_PROP_NAME, Type.DATE, new DateValueFormatter()));
        controller.setResourceTypeTree(new MockResourceTypeTree());

    }

    public Path getRequestPath() {
        requestPath = Path.fromString(atomFeedRequestPath);
        return requestPath;
    }

    public void testCreateFeed() throws Exception {

        // Retrieve the collection to create feed from
        context.checking(new Expectations() {
            {
                one(mockRepository).retrieve(null, requestPath, true);
                will(returnValue(getCollection()));
            }
        });

        // Set main feed id
        final URL url = new URL("http", host, requestPath);
        context.checking(new Expectations() {
            {
                one(mockViewService).constructURL(requestPath);
                will(returnValue(url));
            }
        });

        final String link = requestPath.toString();
        context.checking(new Expectations() {
            {
                one(mockViewService).constructLink(requestPath);
                will(returnValue(link));
            }
        });

        // Set the contenttype on the response
        context.checking(new Expectations() {
            {
                one(mockResponse).setContentType("application/atom+xml;charset=utf-8");
            }
        });

        // Set the writer to use on response
        StringWriter out = new StringWriter();
        final PrintWriter writer = new PrintWriter(out);
        context.checking(new Expectations() {
            {
                one(mockResponse).getWriter();
                will(returnValue(writer));
            }
        });

        
        
        ModelAndView result = controller.handleRequest(mockRequest, mockResponse);

        assertNull("An unexpected model&view was returned", result);

        String feed = out.toString();
        assertTrue("Feed is empty", StringUtils.isNotBlank(feed));

        validateFeedXML(feed);

    }

    private void validateFeedXML(String feed) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();

        Document feedDocument = builder.build(new ByteArrayInputStream(feed.getBytes()));
        assertNotNull(feedDocument);

        Element root = feedDocument.getRootElement();
        assertNotNull("No root document (empty xml document)", root);
        assertEquals("Wrong root element", "feed", root.getName());

        org.jdom.Namespace atomNamespace = org.jdom.Namespace.getNamespace("http://www.w3.org/2005/Atom");

        Element id = root.getChild("id", atomNamespace);
        assertNotNull("No id set for feed", id);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String today = dateFormat.format(Calendar.getInstance().getTime());
        String expectedId = "tag:" + host + "," + today + ":" + atomFeedRequestPath;
        assertEquals("Wrong id", expectedId, id.getText());

        Element title = root.getChild("title", atomNamespace);
        assertNotNull("No title set for feed", title);
        assertEquals("Wrong title", "feedtest", title.getText());

        Element link = root.getChild("link", atomNamespace);
        assertNotNull("No link set for feed", link);
        Attribute linkHref = link.getAttribute("href");
        assertNotNull("No href set for link", linkHref);
        assertEquals("Wrong href", atomFeedRequestPath, linkHref.getValue());
    }

    private Resource getCollection() {
        ResourceImpl collection = new ResourceImpl(requestPath);

        PropertyTypeDefinitionImpl propDef = getPropDef(Namespace.DEFAULT_NAMESPACE, PropertyType.TITLE_PROP_NAME,
                Type.STRING, new StringValueFormatter());
        Property title = propDef.createProperty("feedtest");
        collection.addProperty(title);

        propDef = getPropDef(Namespace.DEFAULT_NAMESPACE, PropertyType.CREATIONTIME_PROP_NAME, Type.DATE,
                new DateValueFormatter());
        Property creationTimeProp = propDef.createProperty(Calendar.getInstance().getTime());
        collection.addProperty(creationTimeProp);

        return collection;
    }

    private class MockSearchComponent implements SearchComponent {

        public Listing execute(HttpServletRequest request, Resource collection, int page, int pageLimit, int baseOffset)
                throws Exception {
            Listing listing = new Listing(null, null, null, 0);

            List<PropertySet> files = new ArrayList<PropertySet>();
            PropertySet event1 = getPropertySet("event1.html");
            files.add(event1);

            listing.setFiles(files);
            return listing;
        }

        private PropertySetImpl getPropertySet(String uri) {
            PropertySetImpl propSet = new PropertySetImpl();

            final Path propSetUri = Path.fromString(requestPath + "/" + uri);
            propSet.setUri(propSetUri);
            final URL url = new URL("http", host, propSetUri);
            context.checking(new Expectations() {
                {
                    one(mockViewService).constructURL(propSetUri);
                    will(returnValue(url));
                }
            });

            context.checking(new Expectations() {
                {
                    one(mockViewService).constructLink(propSetUri);
                    will(returnValue(url.toString()));
                }
            });

            PropertyTypeDefinitionImpl creationTimePropDef = getPropDef(Namespace.DEFAULT_NAMESPACE,
                    PropertyType.CREATIONTIME_PROP_NAME, Type.DATE, new DateValueFormatter());
            propSet.addProperty(creationTimePropDef.createProperty(Calendar.getInstance().getTime()));

            PropertyTypeDefinitionImpl titlePropDef = getPropDef(Namespace.DEFAULT_NAMESPACE,
                    PropertyType.TITLE_PROP_NAME, Type.STRING, new StringValueFormatter());
            propSet.addProperty(titlePropDef.createProperty(uri));

            PropertyTypeDefinitionImpl lastModifiedPropDef = getPropDef(Namespace.DEFAULT_NAMESPACE,
                    PropertyType.LASTMODIFIED_PROP_NAME, Type.DATE, new DateValueFormatter());
            propSet.addProperty(lastModifiedPropDef.createProperty(Calendar.getInstance().getTime()));

            return propSet;
        }

        public Listing execute(HttpServletRequest request, Resource collection, int page, int pageLimit,
                int baseOffset, boolean recursive) throws Exception {
            return null;
        }

    }

}
