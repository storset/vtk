package org.vortikal.scheduling;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanInitializationException;



/**
 * Test case  for {@link org.vortikal.scheduling.SimpleMethodInvokingTriggerBean}.
 * 
 * @author oyviste
 *
 */
public class SimpleMethodInvokingTriggerBeanTest extends TestCase {
    
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
        
        this.triggerCount = 0;
        this.testFailed = false;
        this.triggered = false;
        this.errors = new StringBuffer();
        this.trigger = new SimpleMethodInvokingTriggerBean();
        
    }

    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
    }

    public void testWrongArgumentsProperAbort() throws Exception {
        
        this.trigger.setStartDelay(0);
        this.trigger.setRepeatInterval(50);
        this.trigger.setTargetObject(this);
        this.trigger.setTargetMethodName("triggerMe");
        this.trigger.setBeanName("triggerBean");
        this.trigger.setTriggerThreadName(this.threadName);
        this.trigger.setStartTriggerAfterInitialization(true);
        this.trigger.setAbortTriggerOnTargetMethodException(false);
        
        this.trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        this.trigger.setArguments(new Object[]{new Character('x'), new Object()});
        
        try {
            this.trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        
        // No triggering should have happened because of wrong arguments
        // and the trigger should be disabled
        assertFalse(this.trigger.isEnabled());
        
        assertFalse(this.triggered);
        
    }
    
    public void testTriggeringTargetMethodThrowsException() throws Exception {
        
        this.trigger.setStartDelay(0);
        this.trigger.setRepeatInterval(50);
        this.trigger.setTargetObject(this);
        this.trigger.setTargetMethodName("triggerMeException");
        this.trigger.setBeanName("triggerBean");
        this.trigger.setTriggerThreadName(this.threadName);
        this.trigger.setStartTriggerAfterInitialization(true);
        this.trigger.setAbortTriggerOnTargetMethodException(false);
        
        try {
            this.trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }

        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        this.trigger.stop(true);
        
        // Test that trigger count is at least 2
        assertTrue(this.triggerCount > 1);
        
        this.trigger.setAbortTriggerOnTargetMethodException(true);
        this.triggerCount = 0;
        this.triggered = false;
        
        this.trigger.start();
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        this.trigger.stop(true);
        
        // Test that we got exactly one triggering
        assertEquals(1, this.triggerCount);
        assertTrue(this.triggered);
    }
    
    public void testTriggering() throws Exception {
        
        this.trigger.setStartDelay(0);
        this.trigger.setRepeatInterval(50);
        this.trigger.setTargetObject(this);
        this.trigger.setTargetMethodName("triggerMe");
        this.trigger.setBeanName("triggerBean");
        this.trigger.setTriggerThreadName(this.threadName);
        this.trigger.setArguments(this.args);
        this.trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        this.trigger.setStartTriggerAfterInitialization(true);
        
        try {
            this.trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(250);
        } catch (InterruptedException ie) {}
        
        assertTrue(this.trigger.isEnabled());
        
        this.trigger.stop(false);
        
        assertFalse(this.trigger.isEnabled());
        
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
        this.trigger.setStartDelay(0);
        this.trigger.setRepeatInterval(1);
        this.trigger.setRepeatCount(3);
        this.trigger.setTargetObject(this);
        this.trigger.setTargetMethodName("triggerMe");
        this.trigger.setBeanName("triggerBean");
        this.trigger.setTriggerThreadName(this.threadName);
        this.trigger.setArguments(this.args);
        this.trigger.setArgumentTypes(new Class[]{String.class, Integer.TYPE});
        this.trigger.setStartTriggerAfterInitialization(true);
        
        try {
            this.trigger.afterPropertiesSet(); // Starts the trigger
        } catch (BeanInitializationException e) {
            fail("Failed: " + e.getMessage());
        }
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(100);
        } catch (InterruptedException ie) {}
        
        assertFalse(this.trigger.isEnabled());
        assertEquals(3, this.triggerCount);
        assertTrue(this.triggered);
        
        
        this.trigger.setRepeatCount(0);
        this.triggerCount = 0;
        this.triggered = false;
        
        this.trigger.start();
        
        try { // Give trigger a chance to do its thing
            Thread.sleep(50);
        } catch (InterruptedException ie) {}
        
        assertFalse(this.trigger.isEnabled());
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
        
        if (! this.args[0].equals(arg1)) {
            this.testFailed = true;
            this.errors.append("First argument wrong\n");
        }
        
        int arg2Orig = ((Integer)this.args[1]).intValue();
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
