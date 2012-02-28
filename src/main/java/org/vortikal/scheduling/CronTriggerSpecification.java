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
 * Trigger specification based on cron scheduler expression.
 * 
 * Note that there are six fields for the period specification, contrary
 * to standard cron, which only uses 5 fields (and the sixth for command).
 * This is due to support for seconds in specification (which unix cron does
 * not support).
 * 
 * Expression components:
 * <seconds> <minutes> <hours> <day-of-month> <month> <day of week>
 * 
 * Expression example: "* 10 12 * * mon-tue"
 *
 * (run every monday and tuesday at 10 minutes past 12)
 */
public class CronTriggerSpecification implements TriggerSpecification {
    
    private String expression;

    public CronTriggerSpecification(String expression) {
        if (expression == null) {
            throw new IllegalArgumentException("Cron expression cannot be null");
        }
        this.expression = expression;
    }
    
    public String getExpression() {
        return this.expression;
    }
    
    @Override
    public String toString() {
        return "CronTriggerSpecification[" + this.expression + "]";
    }
}
