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

import org.jmock.Expectations;
import static org.jmock.Expectations.returnValue;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;


/**
 *
 */
public class TaskManagerTest {
    
    private TaskManager tm;
    private ThreadPoolTaskScheduler ts;
    
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    
    @Mock
    private Task task;
    
    public TaskManagerTest() {
        context.setThreadingPolicy(new Synchroniser());
    }
    
    @BeforeClass
    public static void setUpClass() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Log4JLogger");
        System.setProperty("log4j.configuration", "log4j.test.xml");
    }
    
    @Before
    public void setUp() {
        ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(2);
        ts.setThreadNamePrefix("task-manager-thread-");
        ts.setBeanName("test-scheduler");
        ts.afterPropertiesSet();
        
        tm = new TaskManager();
        tm.setTaskScheduler(ts);
    }
    
    @After
    public void tearDown() {
        ts.destroy();
    }
    
    @Test
    public void oneshotScheduledTask() throws InterruptedException {
        final TriggerSpecification oneshot =
                new OneShotTriggerSpecification();
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(oneshot));
            oneOf(task).run();
        }});
        
        tm.scheduleTask(task);
        Thread.sleep(1250);
    }
    
    @Test
    public void simpleTrigger() throws InterruptedException {
        final TriggerSpecification t =
                new PeriodicTriggerSpecification(1, 0, false);
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(t));
            atLeast(2).of(task).run();
        }});
        
        tm.scheduleTask(task);
        Thread.sleep(2250);
    }
    
    @Test
    public void removeTask() throws InterruptedException {
        final OneShotTriggerSpecification oneshot =
                new OneShotTriggerSpecification();
        oneshot.setDelaySeconds(1);
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(oneshot));
        }});
        
        tm.scheduleTask(task);
        Thread.sleep(500);
        tm.removeTask("test-task");
        Thread.sleep(750);
    }
    
    @Test
    public void replaceTask() throws InterruptedException {
        final OneShotTriggerSpecification oneshot =
                new OneShotTriggerSpecification();
        oneshot.setDelaySeconds(1);
        
        final Task replacement = context.mock(Task.class, "replacementTask");
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(oneshot));
            
            allowing(replacement).getId();
            will(returnValue("test-task"));
            allowing(replacement).getTriggerSpecification();
            will(returnValue(null));
        }});

        tm.scheduleTask(task);
        Thread.sleep(250);
        tm.scheduleTask(replacement);
        Thread.sleep(1000);
        
        assertEquals(1, tm.getTaskIds().size());
        assertSame(replacement, tm.getTask("test-task"));
        assertTrue(tm.isDone("test-task"));
    }
    
    @Test
    public void isDoneStatus() throws InterruptedException {
        
        final Task other = context.mock(Task.class, "otherTask");
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(new PeriodicTriggerSpecification(1, 1, true)));
            allowing(task).run();
            
            allowing(other).getId();
            will(returnValue("other"));
            allowing(other).getTriggerSpecification();
            will(returnValue(null));
        }});
        
        tm.scheduleTask(task);
        assertFalse(tm.isDone("test-task"));
        Thread.sleep(1250);
        assertFalse(tm.isDone("test-task"));
        
        tm.scheduleTask(other);
        Thread.sleep(100);
        assertTrue(tm.isDone("other"));
    }
    
    @Test
    public void taskThrowsException() throws InterruptedException {
        
        final Throwable taskFail = new RuntimeException("Task failed");
        
        context.checking(new Expectations(){{
            allowing(task).getId();
            will(returnValue("test-task"));
            allowing(task).getTriggerSpecification();
            will(returnValue(new OneShotTriggerSpecification()));
            allowing(task).run();
            will(throwException(taskFail));
        }});
        
        ts.setErrorHandler(new ErrorHandler(){
            @Override
            public void handleError(Throwable t) {
                assertSame(t, taskFail);
            }
        });
        
        tm.scheduleTask(task);
        Thread.sleep(100);
        assertTrue(tm.isDone("test-task"));
    }
}
