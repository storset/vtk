package org.vortikal.repository.systemjob;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;

public class VideoPosterImageJob extends AbstractExternalVortexMediaServiceJob {

    private PropertyTypeDefinition imagePropDef;

    @Required
    public void setImagePropDef(PropertyTypeDefinition imagePropDef) {
        this.imagePropDef = imagePropDef;
    }

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
                    Resource resource = repository.retrieve(token, path, false);

                    boolean first = true;
                    String parameters = "";
                    for (Entry e : serviceParameters.entrySet()) {
                        if (first)
                            first = false;
                        else
                            parameters += "&";
                        parameters += e.getKey() + "=" + e.getValue();
                    }

                    URL url = new URL(service + repositoryDataDirectory + path.toString() + "?" + parameters);
                    System.out.println("\n\n##\n" + url.toString() + "\n##\n\n");
                    URLConnection conn = url.openConnection();

                    Property property = imagePropDef.createProperty();

                    property.setBinaryValue(StreamUtil.readInputStream(conn.getInputStream()), conn.getContentType());
                    resource.addProperty(property);

                    if (resource.getLock() == null) {
                        // TODO:
                        // resource.removeProperty(thumbnailStatusPropDef);
                        repository.store(token, resource);
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
        });
    }

}
