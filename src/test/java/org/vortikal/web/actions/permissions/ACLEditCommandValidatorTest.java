package org.vortikal.web.actions.permissions;

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

}
