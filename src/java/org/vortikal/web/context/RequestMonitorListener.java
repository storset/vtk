/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.context;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.context.support.RequestHandledEvent;

public class RequestMonitorListener implements ApplicationListener {

    private long interval = 10000;
    private long totalRequests = 0;
    private long requestsSinceLastCompute = 0;
    private float avgRequestsPerSec = 0;
    private long lastComputationTime = System.currentTimeMillis();
    private ComputeThread computeThread;
    

    public RequestMonitorListener() {
        this.computeThread = new ComputeThread();
        this.computeThread.start();
    }
    

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof RequestHandledEvent) {
            this.totalRequests++;
            this.requestsSinceLastCompute++;
        }
    }


    public float getAvgRequestsPerSecond() {
        return this.avgRequestsPerSec;
    }
    

    public long getTotalRequests() {
        return this.totalRequests;
    }
    

    public long getInterval() {
        return this.interval;
    }
    


    private void compute() {

        long curTime = System.currentTimeMillis();

        if (curTime - this.lastComputationTime > this.interval) {
            long timeSinceLastCompute = curTime - this.lastComputationTime;
            this.avgRequestsPerSec = ((float) this.requestsSinceLastCompute
                                      / (float) timeSinceLastCompute) * 1000;

            // Reset counters:
            this.lastComputationTime = curTime;
            this.requestsSinceLastCompute = 0;
        }
    }
    

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("totalRequests: ").append(this.totalRequests);
        sb.append(", avgRequestsPerSecond: ").append(this.avgRequestsPerSec);
        sb.append(" (over " + this.interval + " ms)");
        return sb.toString();
    }
    


    private class ComputeThread extends Thread {

        private boolean alive = true;

        public void run() {

            while (this.alive) {
                try {
                    sleep(RequestMonitorListener.this.interval);
                    compute();
                    
                } catch (InterruptedException e) {
                    this.alive = false;

                } catch (Throwable t) {
                    //logger.warn("Caught exception in compute thread", t);
                }
            }
        }
    }

}
