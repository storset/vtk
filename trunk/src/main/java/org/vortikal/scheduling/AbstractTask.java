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
 *
 */
public abstract class AbstractTask implements Task {

    private TriggerSpecification triggerSpec;
    
    private String id;
    
    private boolean enabled = true;
    
    @Override
    public abstract void run();
    
    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public TriggerSpecification getTriggerSpecification() {
        return this.triggerSpec;
    }

    public void setTriggerSpecification(TriggerSpecification triggerSpec) {
        this.triggerSpec = triggerSpec;
    }
    
    @Override
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "Task[id = " + this.id + ", trigger = " + this.triggerSpec + "]";
    }
    
    /**
     * Set either a simple periodic triggering expression, a oneshot trigger or
     * a cron trigger expression.
     * 
     * Periodic expression consists of up to three fields separated by comma.
     * Only first field (period in seconds) is required, the rest is optional.
     * 
     * First field is period in seconds.
     * Second field is initial delay in seconds (optional).
     * Third field is boolean indicating if period should be fixed rate
     * or fixed delay (optional).
     * 
     * Example: 60,60,true
     * 
     * One shot expression consists of the keyword 'oneshot' in the first
     * field, and optionally the delay in seconds as the second field.
     * 
     * Example: oneshot,100
     * 
     * For cron trigger expression, see {@link CronTriggerSpecification}.
     * 
     * @param expression 
     */
    public void setTriggerExpression(String expression) {
        
        expression = expression.trim();
        
        String[] parts = expression.split("\\s+");
        if (parts.length == 6) {
            // Probably cron trigger spec, use that
            setTriggerSpecification(new CronTriggerSpecification(expression));
            return;
        }

        parts = expression.split("\\s*,\\s*");
        int seconds = -1;
        int delay = 0;
        boolean fixedRate = false;
        
        if (parts.length == 1) {
            if ("oneshot".equals(parts[0])) {
                setTriggerSpecification(new OneShotTriggerSpecification());
                return;
            }
            
            seconds = Integer.parseInt(parts[0]);
        } else if (parts.length == 2) {
            if ("oneshot".equals(parts[0])) {
                OneShotTriggerSpecification one = new OneShotTriggerSpecification();
                one.setDelaySeconds(Integer.parseInt(parts[1]));
                setTriggerSpecification(one);
                return;
            }
            
            seconds = Integer.parseInt(parts[0]);
            delay = Integer.parseInt(parts[1]);
        } else if (parts.length == 3) {
            seconds = Integer.parseInt(parts[0]);
            delay = Integer.parseInt(parts[1]);
            fixedRate = "true".equals(parts[2]) || "1".equals(parts[2]);
        } else {
            throw new IllegalArgumentException("Unable to recognize trigger expression: " + expression);
        }
        
        setTriggerSpecification(new PeriodicTriggerSpecification(seconds, delay, fixedRate));
    }
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks interrupt status.
     * Can be called regularly by tasks which need safe places to abort
     * during execution. Does not clear interrupt status flag.
     * 
     * @throws InterruptedException if interrupt flag for current thread is raised.
     */
    public void checkForInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Interrupted during task execution");
        }
    }

}
