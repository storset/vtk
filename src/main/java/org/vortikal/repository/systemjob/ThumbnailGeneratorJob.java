package org.vortikal.repository.systemjob;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.graphics.ImageService;
import org.vortikal.graphics.ScaledImage;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.security.SecurityContext;

public class ThumbnailGeneratorJob extends RepositoryJob {

    private ImageService imageService;
    private int width;
    private Set<String> supportedFormats;
    private boolean scaleUp = false;
    private long maxSourceImageFileSize = 35000000;
    private long maxSourceImageRawMemoryUsage = 100000000;

    private PropertyTypeDefinition thumbnailPropDef;
    private PropertyTypeDefinition thumbnailStatusPropDef;

    private PathSelector pathSelector;
    private final Log logger = LogFactory.getLog(getClass());
    private boolean continueOnException = true;

    @Override
    public void executeWithRepository(final Repository repository, final SystemChangeContext context) throws Exception {

        if (repository.isReadOnly()) {
            return;
        }

        final String token = SecurityContext.exists() ? SecurityContext.getSecurityContext().getToken() : null;

        this.pathSelector.selectWithCallback(repository, context, new PathSelectCallback() {

            int count = 0;

            int total = -1;

            @Override
            public void beginBatch(int total) {
                this.total = total;
                this.count = 0;
                logger.info("Running job " + getId() + ", " + (this.total >= 0 ? this.total : "?")
                        + " resource(s) selected in batch.");
            }

            @Override
            public void select(Path path) throws Exception {
                ++this.count;
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invoke job " + getId() + " on " + path + " [" + this.count + "/"
                                + (this.total > 0 ? this.total : "?") + "]");
                    }
                    Resource resource = null;
                    try {
                        resource = repository.retrieve(token, path, false);
                    } catch (Exception e) {
                        return;
                    }

                    if (resource == null) {
                        return;
                    }

                    // Check max source content length constraint
                    if (resource.getContentLength() >= maxSourceImageFileSize) {
                        logger.info("Unable to create thumbnail, image size exceeds maximum limit: "
                                + resource.getContentLength());
                        setThumbnailGeneratorStatus(repository, token, resource, "IMAGE_SIZE_EXCEEDS_LIMIT");
                        return;
                    }

                    // Check max source image memory usage constraint
                    Dimension dim = getImageDimension(repository.getInputStream(token, path, true));
                    if (dim != null) {
                        long estimatedMemoryUsage = estimateMemoryUsage(dim);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Estimated memory usage for image of " + dim.width + "x" + dim.height + " = "
                                    + estimatedMemoryUsage + " bytes");
                        }
                        if (estimatedMemoryUsage > maxSourceImageRawMemoryUsage) {
                            logger.warn("Memory usage estimate for source image of dimension " + dim.width + "x"
                                    + dim.height + " exceeds limit of " + maxSourceImageRawMemoryUsage + " bytes.");
                            setThumbnailGeneratorStatus(repository, token, resource, "MEMORY_USAGE_EXCEEDS_LIMIT");
                            return;
                        }
                    }

                    BufferedImage image = ImageIO.read(repository.getInputStream(token, path, true));
                    if (image == null) {
                        return;
                    }
                    Property contentType = resource.getProperty(Namespace.DEFAULT_NAMESPACE,
                            PropertyType.CONTENTTYPE_PROP_NAME);

                    String mimetype = contentType.getStringValue();
                    String imageFormat = mimetype.substring(mimetype.lastIndexOf("/") + 1);

                    if (!supportedFormats.contains(imageFormat.toLowerCase())) {
                        logger.info("Unable to create thumbnail, unsupported format: " + imageFormat);
                        setThumbnailGeneratorStatus(repository, token, resource, "UNSUPPORTED_FORMAT");
                        return;
                    }

                    if (!scaleUp && image.getWidth() <= width) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Will not create a thumbnail: configured NOT to scale up");
                        }
                        setThumbnailGeneratorStatus(repository, token, resource, "CONFIGURED_NOT_TO_SCALE_UP");
                        return;
                    }

                    ScaledImage thumbnail = imageService.scaleImage(image, imageFormat, width, ImageService.HEIGHT_ANY);
                    String thumbnailFormat = !imageFormat.equalsIgnoreCase("jpeg") ? "jpeg" : imageFormat;

                    Property property = thumbnailPropDef.createProperty();
                    property.setBinaryValue(thumbnail.getImageBytes(thumbnailFormat), "image/" + thumbnailFormat);
                    resource.addProperty(property);

                    if (resource.getLock() == null) {
                        resource.removeProperty(thumbnailStatusPropDef);
                        repository.store(token, resource, context);
                        logger.info("Created thumbnail for " + resource);
                    } else {
                        logger.warn("Resource " + resource + " currently locked, will not invoke store.");

                    }
                } catch (ResourceNotFoundException rnfe) {
                    // Resource is no longer there after search (deleted or
                    // moved)
                    logger.warn("A resource (" + path
                            + ") that was to be affected by a systemjob was no longer available: " + rnfe.getMessage());
                } catch (Exception e) {
                    if (continueOnException) {
                        logger.warn("Exception when invoking store for resource " + path, e);
                    } else {
                        throw e;
                    }
                }

                checkForInterrupt();
            }

            private void setThumbnailGeneratorStatus(final Repository repository, final String token,
                    Resource resource, String status) {
                Property statusProp = thumbnailStatusPropDef.createProperty();
                statusProp.setValues(new Value[] { new Value(status,
                        org.vortikal.repository.resourcetype.PropertyType.Type.STRING) });
                resource.addProperty(statusProp);
                try {
                    repository.store(token, resource, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Resource currently locked or moved.. try again in next
                    // batch
                }
            }
        });
    }

    private Dimension getImageDimension(InputStream content) throws Exception {

        ImageInputStream iis = new FileCacheImageInputStream(content, null);
        try {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                reader.dispose();
                return new Dimension(width, height);
            }
        } finally {
            iis.close();
        }

        return null;
    }

    public PathSelector getPathSelector() {
        return pathSelector;
    }

    public void setPathSelector(PathSelector pathSelector) {
        this.pathSelector = pathSelector;
    }

    /**
     * Estimates the raw memory usage for an image where each pixel uses 24 bits
     * or 3 bytes of memory.
     * 
     * @param dim
     *            The <code>Dimension</code> of the image.
     * @return The estimated raw memory usage in bytes.
     */
    private long estimateMemoryUsage(Dimension dim) {
        return (long) dim.height * (long) dim.width * 24 / 8;
    }

    @Required
    public void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    @Required
    public void setWidth(int width) {
        if (width < 1) {
            throw new IllegalArgumentException("scale width must be >= 1");
        }
        this.width = width;
    }

    @Required
    public void setSupportedFormats(Set<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    public void setScaleUp(boolean scaleUp) {
        this.scaleUp = scaleUp;
    }

    public void setMaxSourceImageFileSize(long maxSourceImageFileSize) {
        if (maxSourceImageFileSize < 1) {
            throw new IllegalArgumentException("maxSourceImageFileSize must be >= 1");
        }
        this.maxSourceImageFileSize = maxSourceImageFileSize;
    }

    /**
     * Set cap on estimated raw memory usage on image during scale operation.
     * The estimate is based upon a memory usage of 24 bits per pixel, which
     * should be the most common type. 32bpp images will consume more than
     * actual estimate. To fix that, one needs to provide the bpp value from the
     * {@link org.vortikal.repository.content.ImageContentFactory}.
     * 
     * Default value of 100MB is roughly equivalent to an image of about 33
     * megapixels.
     * 
     * @param maxSourceImageRawMemoryUsage
     */
    public void setMaxSourceImageRawMemoryUsage(long maxSourceImageRawMemoryUsage) {
        if (maxSourceImageRawMemoryUsage < 1) {
            throw new IllegalArgumentException("maxSourceImageRawMemoryUsage must be >= 1");
        }
        this.maxSourceImageRawMemoryUsage = maxSourceImageRawMemoryUsage;
    }

    public void setThumbnailPropDef(PropertyTypeDefinition thumbnailPropDef) {
        this.thumbnailPropDef = thumbnailPropDef;
    }

    public void setThumbnailStatusPropDef(PropertyTypeDefinition thumbnailStatusPropDef) {
        this.thumbnailStatusPropDef = thumbnailStatusPropDef;
    }

}
