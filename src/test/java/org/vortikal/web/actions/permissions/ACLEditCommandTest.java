package org.vortikal.web.actions.permissions;


import static org.junit.Assert.*;
import org.junit.Before;

import org.junit.Test;

public class ACLEditCommandTest {

    private ACLEditCommand command;
    
    @Before
    public void setUp() throws Exception {
        command = new ACLEditCommand("/");
    }
    
    @Test
    public void stripBlanks() {
       String[] values = {" ", "  ", "vrtxadm", "vrtx-core", "Jon Bing", "jonb", null, null, "   "}; 
       String[] noBlanks = command.stripBlanks(values);
       
       assertEquals(4, noBlanks.length);
       assertEquals("vrtxadm", noBlanks[0]); 
       assertEquals("vrtx-core", noBlanks[1]); 
       assertEquals("Jon Bing", noBlanks[2]); 
       assertEquals("jonb", noBlanks[3]); 

    }


}
