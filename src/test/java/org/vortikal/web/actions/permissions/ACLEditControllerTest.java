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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        controller = new ACLEditController();
    }

    @Test
    public void testExtractAndCheckShortcuts() {

        List<Principal> authorizedUsers = new ArrayList<Principal>();
        List<Principal> authorizedGroups = new ArrayList<Principal>();
        List<String> shortcuts = new ArrayList<String>();
        Map<String, List<String>> shortcutsConfig = new HashMap<String, List<String>>();

        // ACLs (which shortcut should be checked)
        Principal userAll = PrincipalFactory.ALL;
        authorizedUsers.add(userAll);

        // Shortcut for privilege (test with all)
        shortcuts.add("all");
        shortcuts.add("all-uio");
        shortcuts.add("all-uni-college");
        shortcuts.add("all-logged-in");
        // None-existing shortcut - should be ignored
        shortcuts.add("all-foo");

        // Shortcut configs
        List<String> groupsUsersAll = new ArrayList<String>();
        groupsUsersAll.add("user:pseudo:all");
        shortcutsConfig.put("all", groupsUsersAll);

        List<String> groupsUsersAllUiO = new ArrayList<String>();
        groupsUsersAllUiO.add("group:alle@uio.no");
        shortcutsConfig.put("all-uio", groupsUsersAllUiO);

        List<String> groupsUsersAllUniCollege = new ArrayList<String>();
        groupsUsersAllUniCollege.add("group:alle@uio.no");
        groupsUsersAllUniCollege.add("group:alle@feide.no");
        shortcutsConfig.put("all-uni-college", groupsUsersAllUniCollege);

        // Invalid user/group prefix - should be ignored
        List<String> groupsUsersAllLoggedIn = new ArrayList<String>();
        groupsUsersAllLoggedIn.add("group:alle@uio.no");
        groupsUsersAllLoggedIn.add("group:alle@feide.uio.no");
        groupsUsersAllLoggedIn.add("foo:alle@webid.uio.no");
        shortcutsConfig.put("all-logged-in", groupsUsersAllLoggedIn);

        try {
            shortcuts = controller.validateShortcuts(shortcuts, shortcutsConfig, null);
            assertEquals(3, shortcuts.size());

            String[][] extractedShortcuts;
            
            extractedShortcuts = controller.extractAndCheckShortcuts(authorizedGroups, authorizedUsers,
                        shortcuts, shortcutsConfig, false);
            assertEquals(3, extractedShortcuts.length);

            assertEquals("checked", extractedShortcuts[0][1]);
            assertEquals("", extractedShortcuts[1][1]);
            assertEquals("", extractedShortcuts[2][1]);

        } catch (Exception ex) {
            System.out.println("Extracting of shortcuts fails " + ex.getMessage());
        }

    }

}
