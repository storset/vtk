package org.vortikal.scheduling;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanInitializationException;



/**
 * Test case  for {@link org.vortikal.scheduling.SimpleMethodInvokingTriggerBean}.
 * 
 * @author oyviste
 *
 */
public class SimpleMethodInvokingTriggerBeanTestCase extends TestCase {
    
    private SimpleMethodInvokingTriggerBean trigger;
    private int triggerCount;
    private boolean testFailed;
    private boolean triggered;
    private StringBuffer errors;
    private String threadName = "test-trigger-thread";
    Object[] args = new Object[]{
            "test-arg-1",
            new Integer(100)
    };
    
    protected void setUp() throws Exception {
        super.setUp();
        
        triggerCount = 0;
        testFailed = false;
        triggered = false;
        errors = new StringBuffer();
        trigger = new SimpleMethodInvokingTriggerBean();
        
    }

    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    public void testWrongArgumentsProperAbort() throws Exception {
        
        trigger.setStartDelay(0);
        trigger.setRepeatInterval(50);
        trigger.setTargetObject(this);
        trigger.setTargetMethodName("triggerMe");
        trigger.setBeanName("triggerBean");
        trigger.setTriggerThreadName(threadName);
        trigger.setStartTriggerAfterInitialization(true);
        trigger.setAbortTriggerOnTargetMethodException(false);
        
        trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        trigger.setArguments(new Object[]{new Character('x'), new Object()});
        
        try {
            trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        
        // No triggering should have happened because of wrong arguments
        // and the trigger should be disabled
        assertFalse(trigger.isEnabled());
        
        assertFalse(this.triggered);
        
    }
    
    public void testTriggeringTargetMethodThrowsException() throws Exception {
        
        trigger.setStartDelay(0);
        trigger.setRepeatInterval(50);
        trigger.setTargetObject(this);
        trigger.setTargetMethodName("triggerMeException");
        trigger.setBeanName("triggerBean");
        trigger.setTriggerThreadName(threadName);
        trigger.setStartTriggerAfterInitialization(true);
        trigger.setAbortTriggerOnTargetMethodException(false);
        
        try {
            trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }

        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        trigger.stop(true);
        
        // Test that trigger count is at least 2
        assertTrue(this.triggerCount > 1);
        
        trigger.setAbortTriggerOnTargetMethodException(true);
        this.triggerCount = 0;
        this.triggered = false;
        
        trigger.start();
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        trigger.stop(true);
        
        // Test that we got exactly one triggering
        assertEquals(1, this.triggerCount);
        assertTrue(this.triggered);
    }
    
    public void testTriggering() throws Exception {
        
        trigger.setStartDelay(0);
        trigger.setRepeatInterval(50);
        trigger.setTargetObject(this);
        trigger.setTargetMethodName("triggerMe");
        trigger.setBeanName("triggerBean");
        trigger.setTriggerThreadName(threadName);
        trigger.setArguments(args);
        trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        trigger.setStartTriggerAfterInitialization(true);
        
        try {
            trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        
        assertTrue(trigger.isEnabled());
        
        trigger.stop(false);
        
        assertFalse(trigger.isEnabled());
        
        if (! this.triggered) {
            fail("Never triggered");
        }
        
        if (this.testFailed) {
            fail("Test failed: " + this.errors.toString());
        }
        
        this.triggered = false;
        try { // Test that we don't get any more triggers
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        
        if (this.triggered) {
            fail("Triggered after being stopped !");
        }
    }
    
    public void testRepeatCount() {
        trigger.setStartDelay(0);
        trigger.setRepeatInterval(1);
        trigger.setRepeatCount(3);
        trigger.setTargetObject(this);
        trigger.setTargetMethodName("triggerMe");
        trigger.setBeanName("triggerBean");
        trigger.setTriggerThreadName(threadName);
        trigger.setArguments(args);
        trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        trigger.setStartTriggerAfterInitialization(true);
        
        try {
            trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(100);
        } catch (InterruptedException ie) {}
        
        assertFalse(trigger.isEnabled());
        assertEquals(3, this.triggerCount);
        assertTrue(this.triggered);
        
        
        trigger.setRepeatCount(0);
        this.triggerCount = 0;
        this.triggered = false;
        
        trigger.start();
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(50);
        } catch (InterruptedException ie) {}
        
        assertFalse(trigger.isEnabled());
        assertEquals(0, this.triggerCount);
        assertFalse(this.triggered);
        
    }
    
    public synchronized void triggerMe(String arg1, int arg2) {
        
        this.triggered = true;
        ++this.triggerCount;
        
        if (! this.threadName.equals(Thread.currentThread().getName())) {
            this.testFailed = true;
            this.errors.append("Thread name not set correctly\n");
        }
        
        if (! args[0].equals(arg1)) {
            this.testFailed = true;
            this.errors.append("First argument wrong\n");
        }
        
        int arg2Orig = ((Integer)args[1]).intValue();
        if (arg2Orig != arg2) {
            this.testFailed = true;
            this.errors.append("Second argument wrong\n");
        }
        
    }
    
    public synchronized void triggerMeException () {
        this.triggered = true;
        ++this.triggerCount;
        
        throw new RuntimeException("Runtime exception from target method");
        
    }
    
}
