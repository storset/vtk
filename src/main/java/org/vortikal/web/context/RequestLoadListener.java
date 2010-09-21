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

public class RequestLoadListener implements ApplicationListener {

    private long totalRequests;
    private int seconds = 60;
    private int[] history = new int[this.seconds];
    private int currentIndex = 0;
    private long lastUpdate = System.currentTimeMillis();
    
    
    public void setHistorySeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Argument must be an integer > 0");
        }
        this.seconds = seconds;
        this.history = new int[seconds];
    }
    

    private void shift() {
        int cur = (this.currentIndex + 1 == this.seconds) 
            ? 0 : this.currentIndex + 1;
        this.history[cur] = 0;
        this.currentIndex = cur;
    }
    

    private int getRelativeIndex(int incr) {
        if (incr > this.seconds || incr < -this.seconds)
            throw new IllegalArgumentException("Invalid increment: " + incr);
        int index = this.currentIndex + incr;
        if (index >= this.seconds) {
            index = index - this.seconds;
        } else if (index < 0) {
            index = this.seconds + index;
        }
        return index;
    }
    
    
    private synchronized void update() {
        long now = System.currentTimeMillis();
        long duration = now - this.lastUpdate;
        if (duration >= 1000) {
            long intervalsToShift = duration / 1000;
            for (int i = 0; i < intervalsToShift && i < this.seconds; i++) {
                shift();
            }
            this.lastUpdate = now;
        }
    }
    

    public int getLoad(int lastSeconds) {
        if (lastSeconds <= 0) throw new IllegalArgumentException(
            "Argument must be a positive integer < " + this.seconds);
        if (lastSeconds > this.seconds) 
            throw new IllegalArgumentException("Request history only spans over "
                                               + this.seconds + " seconds");
        update();

        int total = 0;
        for (int i = 0; i < lastSeconds; i++) {
            total += this.history[getRelativeIndex(-i)];
        }
        return total;
    }
    

    public long getTotalRequests() {
        return this.totalRequests;
    }
    


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof RequestHandledEvent) {
            update();
            this.history[this.currentIndex]++;
            this.totalRequests++;
        }
    }



    @Override
    public String toString() {
        update();

        StringBuilder sb = new StringBuilder();
        sb.append("history for  last ").append(this.seconds);
        sb.append(" seconds\n");

        for (int i = 0; i < this.history.length; i++) {
            int slot = getRelativeIndex(-i);
            sb.append(i).append("      ").append(this.history[slot]);
            sb.append("\n");
        }
        sb.append("total requests: ").append(this.totalRequests);
        return sb.toString();
    }
    



}
