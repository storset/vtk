package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.web.actions.copymove.CopyMoveToSelectedFolderController;

import sun.security.acl.PrincipalImpl;

import junit.framework.TestCase;

public class ACLEditControllerTest extends TestCase {
    
    private ACLEditController controller;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new ACLEditController();
    }
    
    public void testExtractAndCheckShortcuts() {

        List<Principal> authorizedUsers = new ArrayList<Principal>();
        List<Principal> authorizedGroups = new ArrayList<Principal>();
        List<String> shortcuts = new ArrayList<String>();
        
        Principal group = PrincipalFactory.ALL;
        authorizedUsers.add(group);
        
        shortcuts.add("user:pseudo:all");
        
        String[][] extractedShortcuts = controller.extractAndCheckShortcuts(authorizedUsers, authorizedGroups, shortcuts);
        
        assertEquals("user:pseudo:all", extractedShortcuts[0][0]);
        assertEquals("checked", extractedShortcuts[0][1]);
        
    }

}
