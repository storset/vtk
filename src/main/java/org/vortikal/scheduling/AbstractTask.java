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
    
    public void setCronTriggerExpression(String expression) {
        setTriggerSpecification(new CronTriggerSpecification(expression));
    }
    
    /**
     * Expression consisting of three fields separated by comma.
     * Only first field (period in seconds) is required, the rest is optional.
     * 
     * First field is period in seconds.
     * Second field is initial delay in seconds (optional).
     * Third field is boolean indicating if period should be fixed rate
     * or fixed delay (optonal).
     * 
     * @param expression 
     */
    public void setSimplePeriodicTriggerExpression(String expression) {
        String[] parts = expression.trim().split("\\s*,\\s*");
        int seconds = -1;
        int delay = 0;
        boolean fixedRate = false;
        
        if (parts.length == 1) {
            seconds = Integer.parseInt(parts[0]);
        } else if (parts.length == 2) {
            seconds = Integer.parseInt(parts[0]);
            delay = Integer.parseInt(parts[1]);
        } else if (parts.length == 3) {
            seconds = Integer.parseInt(parts[0]);
            delay = Integer.parseInt(parts[1]);
            fixedRate = "true".equals(parts[2]) || "1".equals(parts[2]);
        } else {
            throw new IllegalArgumentException("Invalid simple periodic trigger expression: " + expression);
        }
        
        setTriggerSpecification(new SimplePeriodicTriggerSpecification(seconds, delay, fixedRate));
    }
    
}
