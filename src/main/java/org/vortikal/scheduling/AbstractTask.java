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

import org.springframework.beans.factory.BeanNameAware;

/**
 * Abstract task with default one-shot trigger specification.
 */
public abstract class AbstractTask implements Task, BeanNameAware {

    private TriggerSpecification triggerSpec = new OneShotTriggerSpecification();
    
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
        if (enabled) {
            return this.triggerSpec;
        } else {
            return null;
        }
    }

    /**
     * Set trigger specification for the task.
     * 
     * <p>Default trigger for new instances is {@link OneShotTriggerSpecification}
     * @param triggerSpec the trigger specification instance, or <code>null</code> if
     * no trigger should be provided.
     */
    public void setTriggerSpecification(TriggerSpecification triggerSpec) {
        this.triggerSpec = triggerSpec;
    }

    /**
     * Set task identifier. If this property is unset, then the bean name
     * will be used for identifier.
     * @param id 
     */
    public void setId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        this.id = id;
    }

    @Override
    public String toString() {
        return "AbstractTask{" + "triggerSpec=" + triggerSpec + ", id=" + id + ", enabled=" + enabled + '}';
    }

    /**
     * Set either a simple periodic triggering expression, a oneshot trigger or
     * a cron trigger expression.
     * 
     * <p>Periodic expression consists of up to three fields separated by comma.
     * Only first field (period in seconds) is required, the rest is optional.
     * 
     * <ul>
     * <li>First field is period in seconds.
     * <li>Second field is initial delay in seconds (optional). Default is 0.
     * <li>Third field is boolean indicating if period should be fixed rate (true)
     * or fixed delay (false) (optional). Default is fixed delay since last invocation (false).
     * </ul>
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
     * @param expression the trigger expression to set
     */
    public void setTriggerExpression(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        
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

    /**
     * If id has not been set on task, we use bean name as id.
     * According to {@link BeanNameAware#setBeanName(java.lang.String) setBeanName}, it will
     * be invoked <em>after</em> normal properties are set, so this should be ok.
     * @param name the bean name
     */
    @Override
    public void setBeanName(String name) {
        if (id == null) {
            id = name;
        }
    }
    
    /**
     * Convenience method to flag as task as disabled or enabled.
     * 
     * <p>What this method really
     * does is override the returned trigger based on if it should be disabled
     * or not. If task is disabled, then <code>null</code> will
     * be returned by method {@link #getTriggerSpecification() }, otherwise
     * the normal configured trigger will be returned.
     * 
     * <p>Note that this method cannot be used to control an ongoing task that has already
     * been added to a task manager. It only has an effect at the time the
     * task is added to the task manager, and at future re-schedulings of the
     * task instance.
     * 
     * @param enabled whether to enable this task or not.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check whether this task is currently flagged as enabled or not.
     * @return <code>true</code> if task is enabled and the normal trigger is provided.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Checks interrupt status.
     * Can be called regularly by tasks which need safe places to abort
     * during execution. Clears interrupt status flag.
     * 
     * @throws InterruptedException if interrupt flag for current thread was raised.
     */
    public void checkForInterrupt() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Interrupted during task execution");
        }
    }

}
