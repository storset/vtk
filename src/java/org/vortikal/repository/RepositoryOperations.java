package org.vortikal.repository;

import java.util.HashSet;
import java.util.Set;

public class RepositoryOperations {

    public final static String RETRIEVE = "retrieve";
    public final static String CREATE = "create";
    public final static String CREATE_COLLECTION = "createCollection";
    public final static String COPY = "copy";
    public final static String MOVE = "move";
    public final static String DELETE = "delete";
    public final static String EXISTS = "exists";
    public final static String LOCK = "lock";
    public final static String UNLOCK = "unlock";
    public final static String LIST_CHILDREN = "listChildren";
    public final static String STORE = "store";
    public final static String GET_INPUTSTREAM = "getInputStream";
    public final static String STORE_CONTENT = "storeContent";
    public final static String GET_ACL = "getACL";
    public final static String STORE_ACL = "storeACL";
    
    public static final Set WRITE_OPERATIONS;
    
    static {
        WRITE_OPERATIONS = new HashSet();
        WRITE_OPERATIONS.add(CREATE);
        WRITE_OPERATIONS.add(CREATE_COLLECTION);
        WRITE_OPERATIONS.add(COPY);
        WRITE_OPERATIONS.add(MOVE);
        WRITE_OPERATIONS.add(DELETE);
        WRITE_OPERATIONS.add(LOCK);
        WRITE_OPERATIONS.add(UNLOCK);
        WRITE_OPERATIONS.add(STORE);
        WRITE_OPERATIONS.add(STORE_CONTENT);
        WRITE_OPERATIONS.add(STORE_ACL);
    }

}
