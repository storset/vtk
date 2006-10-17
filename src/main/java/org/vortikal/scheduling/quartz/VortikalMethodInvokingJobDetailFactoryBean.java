/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.scheduling.quartz;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

/**
 * Subclass of MethodInvokingJobDetailFactoryBean whose sole purpose
 * is to set the current thread's name before the method invocation.
 *
 * <p>The thread name during method invocation is the scheduler {@link
 * JobDetail#getGroup group}, followed by a dot ("."), followed by the
 * original thread name. 
 *
 * <p>This class should be removed once there is a way to control
 * thread names in Quartz. (Plus the fact that it is based on
 * "internal" functionality in Spring, which may change.)
 */
public class VortikalMethodInvokingJobDetailFactoryBean
  extends MethodInvokingJobDetailFactoryBean {


    public Object getObject() {

        JobDetail jobDetail = (JobDetail) super.getObject();
        Class jobClass = (jobDetail.getJobClass().isAssignableFrom(StatefulJob.class))
            ? StatefulThreadNameSettingMethodInvokingJob.class
            : ThreadNameSettingMethodInvokingJobWrapper.class;
        jobDetail.setJobClass(jobClass);
        
        return jobDetail;
    }



    public static class ThreadNameSettingMethodInvokingJobWrapper
      extends MethodInvokingJobDetailFactoryBean.MethodInvokingJob {

        protected void executeInternal(JobExecutionContext context)
            throws JobExecutionException {
            
            String originalThreadName = Thread.currentThread().getName();
            String threadName = context.getJobDetail().getGroup()
                + "." + originalThreadName;
            
            try {
                Thread.currentThread().setName(threadName);
                super.executeInternal(context);
            } finally {
                Thread.currentThread().setName(originalThreadName);
            }
        }
    }
    


    public static class StatefulThreadNameSettingMethodInvokingJob
      extends ThreadNameSettingMethodInvokingJobWrapper implements StatefulJob {
    }

}
