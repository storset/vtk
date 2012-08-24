/* Copyright (c) 2012, University of Oslo, Norway
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repository.systemjob;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceNotFoundException;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.security.SecurityContext;

public class MediaMetadataJob extends AbstractExternalVortexMediaServiceJob {

    private MediaMetadataProvider mediaMetadataProvider;

    @Required
    public void setMediaMetadataProvider(MediaMetadataProvider mediaMetadataProvider) {
        this.mediaMetadataProvider = mediaMetadataProvider;
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

                    mediaMetadataProvider.generateMetadata(repository, context, token, path, resource);
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
