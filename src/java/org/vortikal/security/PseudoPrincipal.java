package org.vortikal.security;

public class PseudoPrincipal implements Principal {

    public static final String NAME_AUTHENTICATED = "pseudo:authenticated";
    public static final String NAME_ALL = "pseudo:all";
    public static final String NAME_OWNER = "pseudo:owner";
    
    public static PseudoPrincipal OWNER = 
        new PseudoPrincipal(NAME_OWNER);
    public static PseudoPrincipal ALL = 
        new PseudoPrincipal(NAME_ALL);
    public static PseudoPrincipal AUTHENTICATED = 
        new PseudoPrincipal(NAME_AUTHENTICATED);
    
    private String name;
    
    private PseudoPrincipal(String name) {
        this.name = name;
    }
    
    public static PseudoPrincipal getPrincipal(String name) {
        if (NAME_ALL.equals(name)) return ALL;
        if (NAME_AUTHENTICATED.equals(name)) return AUTHENTICATED;
        if (NAME_OWNER.equals(name)) return OWNER;
        throw new IllegalArgumentException("Pseudo principal with name '"
                + name + "' doesn't exist");
    }
    
    public String getName() {
        return this.name;
    }

    public String getUnqualifiedName() {
        return this.name;
    }

    public String getQualifiedName() {
        return this.name;
    }

    public String getDomain() {
        return null;
    }

    public String getURL() {
        return null;
    }

    public boolean isUser() {
        // XXX: Remove 
        return true;
    }

    public int getType() {
        return Principal.TYPE_PSEUDO;
    }

    public int compareTo(Object o) {
        // XXX: Auto-generated method stub
        return 0;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(this.getClass().getName());
        sb.append(": [").append(this.name).append("]");
        return sb.toString();
    }
    
}
