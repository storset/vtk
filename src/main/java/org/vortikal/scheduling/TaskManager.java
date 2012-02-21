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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Manages all periodic tasks registered in application context.
 * 
 * TODO add some way for tasks to signal abort/cancel or option for aborting
 *      task on error (or delaying it with grace period before retry).
 * 
 * TODO add some methods for stopping/starting/controlling tasks by id.
 */
public class TaskManager implements ApplicationContextAware, InitializingBean {

    private TaskScheduler scheduler;
    
    private ApplicationContext applicationContext;
    
    private final Set<TaskHolder> tasks = new HashSet<TaskHolder>();
    
    private final Log logger = LogFactory.getLog(getClass());
    
    @Override
    public void afterPropertiesSet() {

        Map<String,Task> tasksInContext =
             BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, 
                                                            Task.class, false, false);
        
        for (Map.Entry<String,Task> entry: tasksInContext.entrySet()) {
            Task task = entry.getValue();
            String id = task.getId();
            if (id == null) {
                // Set ID to bean id if it's not explicitly specified in task
                task.setId(entry.getKey());
            }
            final TriggerSpecification triggerSpec = task.getTriggerSpecification();
            if (triggerSpec == null) {
                // Fail early, we don't accept tasks without a valid trigger specification
                throw new BeanInitializationException("Task with id " + task.getId() + " returned null for trigger specification");
            }
            
            tasks.add(new TaskHolder(task, getTriggerImpl(task.getTriggerSpecification())));
        }
        
        scheduleTasks();
    }
    
    private void scheduleTasks() {
        for (TaskHolder th: this.tasks) {
            logger.info("Scheduling task " + th.task.getId() + " with trigger " + th.task.getTriggerSpecification());
            th.future = this.scheduler.schedule(th.task, th.trigger);
        }
    }
    
    private Trigger getTriggerImpl(TriggerSpecification spec) {
        if (spec instanceof SimplePeriodicTriggerSpecification) {
            SimplePeriodicTriggerSpecification s = (SimplePeriodicTriggerSpecification)spec;
            
            PeriodicTrigger p = new PeriodicTrigger(s.getSeconds(), TimeUnit.SECONDS);
            p.setFixedRate(s.isFixedRate());
            if (s.getInitialDelaySeconds() > 0) {
                p.setInitialDelay(s.getInitialDelaySeconds());
            }
            return p;
        }
        
        if (spec instanceof CronTriggerSpecification) {
            CronTriggerSpecification s = (CronTriggerSpecification)spec;
            return new CronTrigger(s.getExpression());
        }
        
        throw new IllegalArgumentException("Unknown trigger specification: " + spec);
    }
    
    private static final class TaskHolder {
        final Task task;
        final Trigger trigger;
        ScheduledFuture future;
        
        TaskHolder(Task task, Trigger trigger) {
            this.task = task;
            this.trigger = trigger;
        }
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Required
    public void setTaskScheduler(TaskScheduler scheduler) {
        this.scheduler = scheduler;
    }
}
