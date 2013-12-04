/* Copyright (c) 2013, University of Oslo, Norway
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

package org.vortikal.videoref;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.SystemChangeContext;
import org.vortikal.repository.systemjob.PathSelectCallback;
import org.vortikal.repository.systemjob.PathSelector;
import org.vortikal.repository.systemjob.StoreResourceJob;

/**
 * Specialized periodic task for handling video updates notifications from videoapp.
 * Queues application events for video updates and processes them one after
 * another in FIFO order. Job will wait for events in its {@link Runnable#run() run method },
 * and it will not finish until interrupted or other error occurs.
 */
public class VideoUpdateTask extends StoreResourceJob {

    private static final int MAX_QUEUED_EVENTS = 100;
    
    private VideoDaoSupport videoDaoSupport;

    private final BlockingQueue<VideoId> eventQueue = 
            new LinkedBlockingQueue<VideoId>(MAX_QUEUED_EVENTS);
    
    private final PathSelectorImpl pathSelector = new PathSelectorImpl();
    
    private final Log logger = LogFactory.getLog(VideoUpdateTask.class.getName());

    public VideoUpdateTask() {
        super.setPathSelector(pathSelector);
    }

    /**
     * Call to notify about an updated video id. It will be added to a queue
     * which is processed by a background thread. All resources referencing the
     * video id will be stored [refreshed] eventually. Video ids are processed
     * in FIFO order.
     *
     * <p>Calls to this method are non-blocking.
     *
     * @param videoId the video id
     * @throws IllegalStateException if the queue of video ids to process is full
     */
    public void videoUpdated(VideoId videoId) throws IllegalStateException {
        if (! eventQueue.offer(videoId)) {
            logger.warn("Event queue full, update notification for video " + videoId + " was discarded");
            throw new IllegalStateException("Video update queue full, try again later.");
        }
    }

    /**
     * Start polling queue for work and process any elements appearing.
     * <p>Queue elements are processed in FIFO order.
     * 
     * <p>This method runs until interrupted or some runtime exception is thrown
     * by the code it invokes. Therefore, it should be called periodically, so
     * errors don't stop the work queue polling entirely.
     */
    @Override
    public void run() {
        // Set up paths for next round before delegating run to super class
        try {
            for (;;) {
                // Blocks on queue until an element becomes available
                VideoId videoToProcess = eventQueue.take();
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing video update event: " + videoToProcess);
                }

                final List<Path> paths = videoDaoSupport.listPaths(videoToProcess);
                pathSelector.setPaths(paths);

                // Store resources at paths to refresh videoref metadata
                if (!paths.isEmpty()) {
                    super.run();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for or processing event");
        }
    }

    private final class PathSelectorImpl implements PathSelector {

        private List<Path> paths;
        
        @Override
        public void selectWithCallback(Repository repository, SystemChangeContext context, PathSelectCallback callback) throws Exception {
            if (paths == null || paths.isEmpty()) {
                callback.beginBatch(0);
                return;
            }
            
            callback.beginBatch(paths.size());
            
            for (Path p: paths) {
                callback.select(p); // Will store resource at path p
            }
        }
        
        void setPaths(List<Path> paths) {
            this.paths = paths;
        }
    }

    /**
     * @param videoDaoSupport the videoDaoSupport to set
     */
    @Required
    public void setVideoDaoSupport(VideoDaoSupport videoDaoSupport) {
        this.videoDaoSupport = videoDaoSupport;
    }
    
    @Override
    public void setPathSelector(PathSelector pathSelector) {
        throw new UnsupportedOperationException("Path selector cannot be configured for this task");
    }
}
