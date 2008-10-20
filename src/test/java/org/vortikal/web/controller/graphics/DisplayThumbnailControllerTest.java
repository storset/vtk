package org.vortikal.web.controller.graphics;

import org.jmock.Expectations;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.ResourceTypeTreeImpl;
import org.vortikal.web.controller.AbstractControllerTest;

public class DisplayThumbnailControllerTest extends AbstractControllerTest {
	
	private DisplayThumbnailController controller;
	
	private Path requestPath;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
        controller = new DisplayThumbnailController();
        controller.setRepository(mockRepository);
	}

	@Override
	protected Path getRequestPath() {
		requestPath = Path.fromString("/images/testimage.gif");
		return requestPath;
	}
	
	public void testDisplayNullThumbnail() throws Exception {
		
		// Retrieve the image to display thumbnail for
		context.checking(new Expectations() {{ one(mockRepository).retrieve(null, requestPath, true); will(returnValue(getImage(false))); }});
		
		// No thumbnail, so we should redirect
		context.checking(new Expectations() {{ one(mockResponse).sendRedirect(requestPath.toString()); }});
		
		ModelAndView result = controller.handleRequest(mockRequest, mockResponse);
		
		assertNull("Unexpected model&view was returned", result);
	}
	
	private Resource getImage(boolean withThumbnail) {
		ResourceImpl image = new ResourceImpl();
		image.setUri(requestPath);
		image.setResourceTypeTree(new ResourceTypeTreeImpl());
		
		if (withThumbnail) {
			// TODO create binary property and add to image
		}
        
		return image;
	}

}
