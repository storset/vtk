package org.vortikal.repository.store.jcr;


/**
 * Various JCR DAO related constant strings.
 *
 */
public class JcrDaoConstants {

    // The resource tree root node path
    public static final String VRTX_ROOT = "/vrtx";
    
    // The lock tree root node path
    public static final String LOCKS_ROOT = "/locks";

    // Resource items
    public static final String VRTX_HIERARCHY_NODE_NAME = "vrtx:hierarchyNode";
    public static final String VRTX_PREFIX = "vrtx:";
    public static final String VRTX_PREFIX_SEPARATOR = ";";
    public static final String VRTX_FILE_NAME = "vrtx:file";
    public static final String VRTX_FOLDER_NAME = "vrtx:folder";
    public static final String CONTENT = "vrtx:content";
    public static final String RESOURCE_TYPE = "vrtx:resourceType";
    public static final String VRTX_LOCK_NAME = "vrtx:lock";
    
    // Auxiliary resource items used for searching-purposes.
    public static final String RESOURCE_NAME = "vrtx:resourceName";
    
    // Acl item names
    public static final String VRTX_ACL_NAME = "vrtx:acl";
    public static final String VRTX_ACTION_NAME = "vrtx:action";
    public static final String VRTX_PRINCIPAL_NAME = "vrtx:principal";
    public static final String VRTX_PRINCIPAL_TYPE_NAME = "vrtx:principalType";

    // Comment item names
    public static final String VRTX_COMMENTS_NAME = "vrtx:comments";
    public static final String VRTX_COMMENT_NAME = "vrtx:comment";
    public static final String VRTX_COMMENT_AUTHOR = "vrtx:commentAuthor";
    public static final String VRTX_COMMENT_TITLE = "vrtx:commentTitle";
    public static final String VRTX_COMMENT_TIME = "vrtx:commentTime";
    public static final String VRTX_COMMENT_BODY = "vrtx:commentBody";

    public static final String ROOT_USER = "root@localhost";

}
