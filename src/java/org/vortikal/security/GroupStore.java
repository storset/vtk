package org.vortikal.security;

import org.springframework.core.Ordered;

public interface GroupStore extends Ordered {

    /**
     * Validates the existence of a given group.
     *
     * @param group the group to validate
     * @return <code>true</code> if the group exists,
     * <code>false</code> otherwise.
     */
    public boolean validateGroup(Principal group)
        throws AuthenticationProcessingException;



    /**
     * Lists the members of a group.
     *
     * @param groupName the group in question
     * @return an array of the principals that are members of the group
     */
//    public String[] resolveGroup(Principal group)
//        throws AuthenticationProcessingException;
    
    
    /**
     * Convenience method for determining whether a principal is a
     * member of a group.
     *
     * @param principal the name of the principal
     * @param group the group in question 
     * @return true if the group exists and the given principal is a
     * member of that group, false otherwise.
     */
    public boolean isMember(Principal principal, Principal group);

}
