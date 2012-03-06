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

package org.vortikal.scheduling;

/**
 * Thin abstraction for 
 */
public class PeriodicTriggerSpecification implements TriggerSpecification {

    private int seconds;
    private int initialDelaySeconds;
    private boolean fixedRate = false;
    
    /**
     * 
     * @param seconds The seconds between each period
     * @param fixedRate if seconds should be interpreted as fixed rate insteead of
     *                  fixed delay between each invocation.
     */
    public PeriodicTriggerSpecification(int seconds, int initialDelaySeconds, boolean fixedRate) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("seconds must be >= 1");
        }
        if (initialDelaySeconds < 0) {
            initialDelaySeconds = 0;
        }
        this.seconds = seconds;
        this.initialDelaySeconds = initialDelaySeconds;
        this.fixedRate = fixedRate;
    }
    
    public int getSeconds() {
        return this.seconds;
    }
    
    public int getInitialDelaySeconds() {
        return this.initialDelaySeconds;
    }
    
    public boolean isFixedRate() {
        return this.fixedRate;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getClass().getSimpleName());
        b.append("[").append("period = ").append(this.seconds).append(" sec");
        b.append(", delay = ").append(this.initialDelaySeconds).append(" sec");
        b.append(this.fixedRate ? ", fixed rate" : ", fixed delay");
        b.append("]");
        return b.toString();
    }
}
