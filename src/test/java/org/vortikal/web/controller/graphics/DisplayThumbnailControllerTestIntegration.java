package org.vortikal.web.controller.graphics;

import java.io.IOException;

import org.jmock.Expectations;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceTypeTreeImpl;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.web.controller.AbstractControllerTest;

public class DisplayThumbnailControllerTestIntegration extends AbstractControllerTest {
	
	private DisplayThumbnailController controller;
	private Path requestPath;
	
	protected final Property mockThumbnail = context.mock(Property.class);
	private final String mimeType = "image/png";
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
        controller = new DisplayThumbnailController();
        controller.setRepository(mockRepository);
	}

	@Override
	protected Path getRequestPath() {
		requestPath = Path.fromString("/images/testImage.gif");
		return requestPath;
	}
	
	public void testDisplayNullThumbnail() throws Exception {
		prepareRequest(false, true);
		
		// No thumbnail, so we should redirect
		context.checking(new Expectations() {{ one(mockResponse).sendRedirect(requestPath.toString()); }});
		
		handleRequest();
	}
	
	public void testDisplayNoMimeTypeThumbnail() throws Exception {
		prepareRequest(true, true);
		
		// No mimetype for binary data, so we should redirect
		context.checking(new Expectations() {{ one(mockThumbnail).getBinaryMimeType(); will(returnValue("")); }});
		
		handleRequest();
	}
	
	public void testDisplayNullBytestreamThumbnail() throws Exception {
		prepareRequest(true, true);
		
		// No binarystream, so we should redirect
		context.checking(new Expectations() {{ one(mockThumbnail).getBinaryMimeType(); will(returnValue(mimeType)); }});
		context.checking(new Expectations() {{ one(mockThumbnail).getBinaryStream(); will(returnValue(null)); }});

		handleRequest();
	}
	
	private void prepareRequest(final boolean withThumbnail, boolean expectRedirect) throws IOException {
		// Retrieve the image to display thumbnail for
		context.checking(new Expectations() {{ one(mockRepository).retrieve(null, requestPath, true); will(returnValue(getImage(withThumbnail))); }});
		if (expectRedirect) {
			context.checking(new Expectations() {{ one(mockResponse).sendRedirect(requestPath.toString()); }});
		}
	}

	private void handleRequest() throws Exception {
		ModelAndView result = controller.handleRequest(mockRequest, mockResponse);
		assertNull("Unexpected model&view was returned", result);
	}

	private Resource getImage(boolean withThumbnail) throws IOException {
		ResourceImpl image = new ResourceImpl();
		image.setUri(requestPath);
		image.setResourceTypeTree(new ResourceTypeTreeImpl());
		
		if (withThumbnail) {
			final PropertyTypeDefinitionImpl thumbnailPropDef = new PropertyTypeDefinitionImpl();
	        thumbnailPropDef.setType(Type.BINARY);
	        thumbnailPropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
	        thumbnailPropDef.setName(PropertyType.THUMBNAIL_PROP_NAME);
	        
	        context.checking(new Expectations() {{ one(mockThumbnail).getDefinition(); will(returnValue(thumbnailPropDef)); }});
	        
			image.addProperty(mockThumbnail);
		}
        
		return image;
	}

}
