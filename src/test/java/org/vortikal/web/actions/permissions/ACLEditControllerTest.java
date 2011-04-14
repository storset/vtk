package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

public class ACLEditControllerTest extends TestCase {
    
    private ACLEditController controller;
    private PrincipalFactory principalFactory;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new ACLEditController();
        principalFactory = new PrincipalFactory();
    }
    
    @Test
    public void testExtractAndCheckShortcuts() {

//        List<Principal> authorizedUsers = new ArrayList<Principal>();
//        List<Principal> authorizedGroups = new ArrayList<Principal>();
//        List<String> shortcuts = new ArrayList<String>();
//        Map<String, List<String>> shortcutsConfig = new HashMap<String, List<String>>(); 
//        
//        // ACLs
//        Principal userAll = PrincipalFactory.ALL;
//        authorizedUsers.add(userAll);
//
//        // Shortcuts
//        List<String> groupsUsersAll = new ArrayList<String>();
//        groupsUsersAll.add("user:pseudo:all");
//        shortcutsConfig.put("all", groupsUsersAll);
//        
//        List<String> groupsUsersAllUiO = new ArrayList<String>();
//        groupsUsersAllUiO.add("group:alle@uio.no");
//        shortcutsConfig.put("all-uio", groupsUsersAllUiO);
//        
//        List<String> groupsUsersAllUniCollege = new ArrayList<String>();
//        groupsUsersAllUniCollege.add("group:alle@uio.no");
//        groupsUsersAllUniCollege.add("group:alle@feide.no");
//        shortcutsConfig.put("all-uio", groupsUsersAllUniCollege);
//        
//        List<String> groupsUsersAllLoggedIn = new ArrayList<String>();
//        groupsUsersAllLoggedIn.add("group:alle@uio.no");
//        groupsUsersAllLoggedIn.add("group:alle@feide.uio.no");
//        groupsUsersAllLoggedIn.add("alle@webid.uio.no");
//        shortcutsConfig.put("all-uio", groupsUsersAllLoggedIn );
//        
//        int validShortcuts = controller.countValidshortcuts(shortcuts);
//        assertEquals(4, validShortcuts);
//        
//        String[][] extractedShortcuts = controller.extractAndCheckShortcuts(authorizedUsers, authorizedGroups, shortcuts, validShortcuts);
//        assertEquals(4, extractedShortcuts.length);

    }

}
