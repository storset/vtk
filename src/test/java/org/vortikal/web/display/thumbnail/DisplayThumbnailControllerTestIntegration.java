package org.vortikal.web.display.thumbnail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;

import org.jmock.Expectations;
import org.springframework.web.servlet.ModelAndView;
import org.vortikal.repository.ContentStream;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.resourcetype.PropertyTypeDefinitionImpl;
import org.vortikal.web.AbstractControllerTest;

public class DisplayThumbnailControllerTestIntegration extends AbstractControllerTest {

    private DisplayThumbnailController controller;
    private Path requestPath;

    protected final Property mockThumbnail = context.mock(Property.class);
    private final String thumbnailMimeType = "image/png";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new DisplayThumbnailController();
    }

    @Override
    protected Path getRequestPath() {
        requestPath = Path.fromString("/images/testImage.gif");
        return requestPath;
    }

    public void testDisplayNullThumbnail() throws Exception {
        prepareRequest(false, true);

        // No thumbnail, so we should redirect
        context.checking(new Expectations() {
            {
                one(mockResponse).sendRedirect(requestPath.toString());
            }
        });

        handleRequest();
    }

    public void testDisplayNoMimeTypeThumbnail() throws Exception {
        prepareRequest(true, true);

        // No mimetype for binary data, so we should redirect
        context.checking(new Expectations() {
            {
                one(mockThumbnail).getBinaryContentType();
                will(returnValue(""));
            }
        });

        handleRequest();
    }

    public void testDisplayThumbnail() throws Exception {
        prepareRequest(true, false);

        BufferedImage image = ImageIO.read(this.getClass().getResourceAsStream("testImage.gif"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        final byte[] imageBytes = out.toByteArray();
        out.close();
        InputStream in = new ByteArrayInputStream(imageBytes);
        final ContentStream contentStream = new ContentStream(in, imageBytes.length);

        context.checking(new Expectations() {
            {
                atLeast(2).of(mockThumbnail).getBinaryContentType();
                will(returnValue(thumbnailMimeType));
            }
        });
        context.checking(new Expectations() {
            {
                one(mockThumbnail).getBinaryStream();
                will(returnValue(contentStream));
            }
        });
        context.checking(new Expectations() {
            {
                one(mockResponse).setContentType(thumbnailMimeType);
            }
        });
        context.checking(new Expectations() {
            {
                one(mockResponse).setContentLength(imageBytes.length);
            }
        });

        final ServletOutputStream responseOut = new MockServletOutputStream();
        context.checking(new Expectations() {
            {
                one(mockResponse).getOutputStream();
                will(returnValue(responseOut));
            }
        });

        handleRequest();
    }

    private void prepareRequest(final boolean withThumbnail, boolean expectRedirect) throws Exception {
        // Retrieve the image to display thumbnail for
        context.checking(new Expectations() {
            {
                one(mockRepository).retrieve(null, requestPath, true);
                will(returnValue(getImage(withThumbnail)));
            }
        });
        if (expectRedirect) {
            context.checking(new Expectations() {
                {
                    one(mockResponse).sendRedirect(requestPath.toString());
                }
            });
        }
    }

    private void handleRequest() throws Exception {
        ModelAndView result = controller.handleRequest(mockRequest, mockResponse);
        assertNull("Unexpected model&view was returned", result);
    }

    private Resource getImage(boolean withThumbnail) throws IOException {
        ResourceImpl image = new ResourceImpl(requestPath);

        if (withThumbnail) {
            final PropertyTypeDefinitionImpl thumbnailPropDef = new PropertyTypeDefinitionImpl();
            thumbnailPropDef.setType(Type.BINARY);
            thumbnailPropDef.setNamespace(Namespace.DEFAULT_NAMESPACE);
            thumbnailPropDef.setName(PropertyType.THUMBNAIL_PROP_NAME);

            context.checking(new Expectations() {
                {
                    one(mockThumbnail).getDefinition();
                    will(returnValue(thumbnailPropDef));
                }
            });

            image.addProperty(mockThumbnail);
        }

        return image;
    }

    private class MockServletOutputStream extends ServletOutputStream {

        @Override
        public void write(int b) throws IOException {
        }

    }

}
