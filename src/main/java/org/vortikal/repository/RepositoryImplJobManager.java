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

package org.vortikal.repository;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Manager for periodic repository-internal jobs.
 */
public final class RepositoryImplJobManager implements Runnable {

    private RepositoryImpl repository;
    private int periodInterval = 600;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Date purgeTrashLastRun;
    private Date revisionGCLastRun;
    private Set<Integer> revisionGCHours = new HashSet<Integer>(Arrays.asList(new Integer[]{3}));
    private Set<Integer> trashCanPurgeHours = new HashSet<Integer>(Arrays.asList(new Integer[]{4}));
    
    private final Log periodicLogger = LogFactory.getLog(getClass());
    
    // Start background jobs
    public void start() {
        periodicLogger.info("Start repository job manager.");
        this.executor.scheduleAtFixedRate(this, 10, this.periodInterval, TimeUnit.SECONDS);
    }

    // Shutdown background jobs
    public void stop() {
        periodicLogger.info("Shutdown repository job manager.");
        this.executor.shutdownNow();
    }
    
    @Override
    public void run() {
        if (repository.isReadOnly()) return;
        
        // Delete expired locks at every period
        try {
            repository.deleteExpiredLocks();
        } catch (Throwable t) {
            periodicLogger.error("Error while deleting expired locks", t);
        }

        // Trash purging at certain hours of the day
        try {
            if (shouldRun(purgeTrashLastRun, trashCanPurgeHours)) {
                periodicLogger.info("Executing trash purge");
                repository.purgeTrash();
                purgeTrashLastRun = new Date();
            }
        } catch (Throwable t) {
            periodicLogger.error("Error while purging trash", t);
        }

        // Content revision GC at certain hours of the day
        try {
            if (shouldRun(revisionGCLastRun, revisionGCHours)) {
                periodicLogger.info("Executing revision store garbage collection.");
                repository.revisionStoreGc();
                revisionGCLastRun = new Date();
            }
        } catch (Throwable t) {
            periodicLogger.error("Error while running content revision garbage collection", t);
        }
    }
    
    // Determine if periodic job should be executed, when it is supposed to be run
    // once within certain hours of the day.
    private boolean shouldRun(Date lastRun, Set<Integer> runAtHours) {
        Calendar nowCal = Calendar.getInstance();

        if (runAtHours.contains(nowCal.get(Calendar.HOUR_OF_DAY))) {
            if (lastRun == null) {
                return true;
            }

            Calendar lastRunCal = Calendar.getInstance();
            lastRunCal.setTime(lastRun);
            if (lastRunCal.get(Calendar.HOUR_OF_DAY) != nowCal.get(Calendar.HOUR_OF_DAY)) {
                return true;
            }
            // Same hour of day, but maybe different day ..
            if (lastRunCal.get(Calendar.DAY_OF_MONTH) != nowCal.get(Calendar.DAY_OF_MONTH)) {
                return true;
            }
        }

        return false;
    }

    @Required
    public void setRepository(RepositoryImpl repository) {
        this.repository = repository;
    }

    public void setPeriodInterval(int periodInterval) {
        this.periodInterval = periodInterval;
    }

    public void setRevisionGCHours(Set<Integer> revisionGCHours) {
        this.revisionGCHours = revisionGCHours;
    }

    public void setTrashCanPurgeHours(Set<Integer> trashCanPurgeHours) {
        this.trashCanPurgeHours = trashCanPurgeHours;
    }

}
