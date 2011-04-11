package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;

public class ACLEditCommandValidatorTest extends TestCase {

    private ACLEditCommandValidator validator;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        validator = new ACLEditCommandValidator();
    }
    
    @Test
    public void testToCSV() {
        
        String csv = "'alle@uio.no', 'vrtxadm', 'vrtx-core'";
        
        csv += validator.toCSV(csv, "vrtxphp"); // add
        assertEquals("'alle@uio.no', 'vrtxadm', 'vrtx-core', 'vrtxphp'", csv);
        
        csv += validator.toCSV(csv, "vrtxadm"); // ignore duplicate
        assertEquals("'alle@uio.no', 'vrtxadm', 'vrtx-core', 'vrtxphp'", csv);
 
    }
    
    @Test
    public void testGetAc_userName() {
        
        String[] ac_userNames = {"Jon Bing;jonb", "Jon Skolmen;jons"};
        List<String> userNameEntries = new ArrayList<String>();
        
        assertEquals("jonb", validator.getAc_userName("Jon Bing", ac_userNames, userNameEntries));
        assertEquals("jons", validator.getAc_userName("Jon Skolmen", ac_userNames, userNameEntries));
        
        userNameEntries.add("jonb");
        userNameEntries.add("jons");
        
        assertNull(validator.getAc_userName("Jon Bing", ac_userNames, userNameEntries));
        assertNull(validator.getAc_userName("Jon Skolmen", ac_userNames, userNameEntries));
        
    }

}
