package org.vortikal.web.actions.permissions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import junit.framework.TestCase;

public class ACLEditBinderTest extends TestCase {
    
    private ACLEditBinder binder;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        binder = new ACLEditBinder("","");
    }
    
    @Test
    public void testExtractGroupsAndUsersForRemoval() {
        
       List<String> removedUsers = new ArrayList<String>();
       List<String> removedGroups = new ArrayList<String>();
       List<String> params = new ArrayList<String>();
       
       params.add("removeUser.oyvihatl");
       params.add("removeUser.pseudo:all");
       
       params.add("removeGroup.vrtx-core");
       params.add("removeGroup.alle@uio.no");
       params.add("removeGroup.alle@feide.no");
       params.add("removeGroup.alle@webid.uio.no");
       
       params.add("foobar.vrtxadm"); // invalid - should be ignored
        
       binder.extractGroupsAndUsersForRemoval(removedUsers, removedGroups, params);
       
       assertEquals(2, removedUsers.size());
       assertEquals("oyvihatl", removedUsers.get(0));
       assertEquals("pseudo:all", removedUsers.get(1));
       
       assertEquals(4, removedGroups.size());
       assertEquals("vrtx-core", removedGroups.get(0));
       assertEquals("alle@uio.no", removedGroups.get(1));
       assertEquals("alle@feide.no", removedGroups.get(2));
       assertEquals("alle@webid.uio.no", removedGroups.get(3));
  
    }

}
