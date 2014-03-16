package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;

import org.junit.Test;

import static org.junit.Assert.*;

public class ACLEditCommandValidatorTest {

    private ACLEditCommandValidator validator;
    
    @Before
    public void setUp() throws Exception {
        validator = new ACLEditCommandValidator();
    }
    
    @Test
    public void toCSV() {
        
        String csv = "'alle@uio.no', 'vrtxadm', 'vrtx-core'";
        
        csv += validator.toCSV(csv, "vrtxphp"); // add
        assertEquals("'alle@uio.no', 'vrtxadm', 'vrtx-core', 'vrtxphp'", csv);
        
        csv += validator.toCSV(csv, "vrtx,test"); // add with comma
        assertEquals("'alle@uio.no', 'vrtxadm', 'vrtx-core', 'vrtxphp', 'vrtx,test'", csv);
 
    }
    
    @Test
    public void getAc_userName() {
        
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
