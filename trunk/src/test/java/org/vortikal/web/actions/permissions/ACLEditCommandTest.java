package org.vortikal.web.actions.permissions;

import junit.framework.TestCase;

import org.junit.Test;

public class ACLEditCommandTest extends TestCase {

    private ACLEditCommand command;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        command = new ACLEditCommand("/");
    }
    
    @Test
    public void testStripBlanks() {
       String[] values = {" ", "  ", "vrtxadm", "vrtx-core", "Jon Bing", "jonb", null, null, "   "}; 
       String[] noBlanks = command.stripBlanks(values);
       
       assertEquals(4, noBlanks.length);
       assertEquals("vrtxadm", noBlanks[0]); 
       assertEquals("vrtx-core", noBlanks[1]); 
       assertEquals("Jon Bing", noBlanks[2]); 
       assertEquals("jonb", noBlanks[3]); 

    }


}
