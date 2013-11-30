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

package org.vortikal.scheduling;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class AbstractTaskTest {

    private class Task extends AbstractTask {
        @Override
        public void run() {
        }
    }
    
    private Task task;
    
    @Before
    public void setUp() {
        task = new Task();
    }

    @Test
    public void taskIdOverrideBeanName() {
        task.setId("a");
        task.setBeanName("b");
        assertEquals("a", task.getId());
    }

    @Test
    public void taskIdFromBeanName() {
        task.setBeanName("b");
        assertEquals("b", task.getId());
    }
    
    @Test
    public void defaultTriggerNotNull() {
        assertNotNull(task.getTriggerSpecification());
    }
    
    @Test
    public void nullTriggerWhenDisabled() {
        task.setEnabled(false);
        assertFalse(task.isEnabled());
        assertNull(task.getTriggerSpecification());
        
        task.setEnabled(true);
        assertTrue(task.isEnabled());
        assertNotNull(task.getTriggerSpecification());
    }
    
    @Test
    public void triggerExpressionOneShot() {
        task.setTriggerExpression("oneshot");
        assertTrue(task.getTriggerSpecification() instanceof OneShotTriggerSpecification);
        OneShotTriggerSpecification oneshot = (OneShotTriggerSpecification)task.getTriggerSpecification();
        assertEquals(0, oneshot.getDelaySeconds());
        
        task.setTriggerExpression("oneshot,10");
        assertTrue(task.getTriggerSpecification() instanceof OneShotTriggerSpecification);
        oneshot = (OneShotTriggerSpecification)task.getTriggerSpecification();
        assertEquals(10, oneshot.getDelaySeconds());
    }
    
    @Test
    public void triggerExpressionSimple() {
        task.setTriggerExpression("10");
        assertTrue(task.getTriggerSpecification() instanceof PeriodicTriggerSpecification);
        PeriodicTriggerSpecification pts = (PeriodicTriggerSpecification)task.getTriggerSpecification();
        assertEquals(10, pts.getSeconds());
        assertEquals(0, pts.getInitialDelaySeconds());
        assertFalse(pts.isFixedRate());
        
        task.setTriggerExpression("10,15");
        assertTrue(task.getTriggerSpecification() instanceof PeriodicTriggerSpecification);
        pts = (PeriodicTriggerSpecification)task.getTriggerSpecification();
        assertEquals(10, pts.getSeconds());
        assertEquals(15, pts.getInitialDelaySeconds());
        assertFalse(pts.isFixedRate());

        task.setTriggerExpression("10,15,true");
        assertTrue(task.getTriggerSpecification() instanceof PeriodicTriggerSpecification);
        pts = (PeriodicTriggerSpecification)task.getTriggerSpecification();
        assertEquals(10, pts.getSeconds());
        assertEquals(15, pts.getInitialDelaySeconds());
        assertTrue(pts.isFixedRate());
    }
    
    @Test
    public void triggerExpressionCron() {
        task.setTriggerExpression("00 10 12 * * mon-tue");
        assertTrue(task.getTriggerSpecification() instanceof CronTriggerSpecification);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void invalidExpression() {
        task.setTriggerExpression("x");
    }
}
