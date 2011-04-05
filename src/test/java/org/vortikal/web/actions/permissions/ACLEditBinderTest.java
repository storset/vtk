package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ACLEditBinderTest extends TestCase {
    
    private ACLEditBinder binder;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        binder = new ACLEditBinder("","");
    }
    
    public void testExtractGroupsAndUsersForRemoval() {
        
       List<String> removedUsers = new ArrayList<String>();
       List<String> removedGroups = new ArrayList<String>();
       List<String> params = new ArrayList<String>();
       
       params.add("removeUser.oyvihatl");
       params.add("removeGroup.vrtx-core");
        
       binder.extractGroupsAndUsersForRemoval(removedUsers, removedGroups, params);
       
       assertEquals("oyvihatl", removedUsers.get(0));
       assertEquals("vrtx-core", removedGroups.get(0));
       
    }

}
