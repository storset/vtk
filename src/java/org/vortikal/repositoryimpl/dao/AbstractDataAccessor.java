/* Copyright (c) 2004, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.vortikal.repositoryimpl.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repositoryimpl.PropertyManagerImpl;
import org.vortikal.repositoryimpl.ResourceImpl;
import org.vortikal.security.PrincipalManager;


public abstract class AbstractDataAccessor
  implements InitializingBean, DataAccessor {

    protected Log logger = LogFactory.getLog(this.getClass());

    protected ContentStore contentStore;
    protected DataSource dataSource;
    protected PropertyManagerImpl propertyManager;
    protected PrincipalManager principalManager;
    
    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void setPropertyManager(PropertyManagerImpl propertyManager) {
        this.propertyManager = propertyManager;
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }
    
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public void afterPropertiesSet() throws Exception {
        if (this.contentStore == null) {
            throw new BeanInitializationException(
                "JavaBean property 'contentStore' not specified");
        }
        if (this.propertyManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'propertyManager' not specified");
        }
        if (this.principalManager == null) {
            throw new BeanInitializationException(
                "JavaBean property 'principalManager' not specified");
        }
        if (this.dataSource == null) {
            throw new BeanInitializationException(
                "JavaBean property 'dataSource' not specified");
        }
    }



    public boolean validate() throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            boolean ok = validate(conn);
            conn.commit();
            return ok;

        } catch (SQLException e) {
            logger.warn("Error occurred while checking database validity: ", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public ResourceImpl load(String uri) throws IOException {
        Connection conn = null;
        ResourceImpl retVal = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            retVal = load(conn, uri);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while loading resource(s)", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }


    public void deleteExpiredLocks() throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            deleteExpiredLocks(conn);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while deleting expired locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    


    public void addChangeLogEntry(String loggerID, String loggerType, String uri,
                                  String operation, int resourceId, boolean collection,
                                  boolean recurse) throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            addChangeLogEntry(conn, loggerID, loggerType, uri, operation, resourceId,
                              collection, recurse);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while adding changelog entry for " + uri, e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] discoverLocks(ResourceImpl resource)
            throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            String[] lockedURIs = discoverLocks(conn, resource);
            conn.commit();

            return lockedURIs;
        } catch (SQLException e) {
            logger.warn("Error occurred while discovering locks", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public String[] listSubTree(ResourceImpl parent)
            throws IOException {
        Connection conn = null;
        String[] retVal = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            retVal = listSubTree(conn, parent);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while listing resource tree", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }


    public void store(ResourceImpl r)
            throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            store(conn, r);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while storing resource " + r.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public void delete(ResourceImpl resource)
            throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            delete(conn, resource);
            conn.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    public ResourceImpl[] loadChildren(ResourceImpl parent)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("load children:" + parent.getURI());
        }
        Connection conn = null;
        ResourceImpl[] retVal = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            retVal = loadChildren(conn, parent);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred while loading children: " + parent.getURI(), e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }


    public String[] discoverACLs(ResourceImpl resource) throws IOException {
        Connection conn = null;
        String[] retVal = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            retVal = discoverACLs(conn, resource);
            conn.commit();
        } catch (SQLException e) {
            logger.warn("Error occurred finding ACLs ", e);
            throw new IOException(e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        return retVal;
    }


    public void copy(ResourceImpl resource, String destURI, boolean copyACLs,
                     boolean setOwner, String owner) throws IOException {
        Connection conn = null;

        try {
            conn = this.dataSource.getConnection();
            conn.setAutoCommit(false);     
            copy(conn, resource, destURI, copyACLs, setOwner, owner);
            conn.commit();
            
        } catch (SQLException e) {
            logger.warn("Error occurred while copying resource " + resource, e);
            throw new IOException(e.getMessage());
        } catch (IOException e) {
            logger.warn("Error occurred while copying resource " + resource, e);
            throw e;
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }
    }


    // To be implemented by subclasses:


    protected abstract boolean validate(Connection conn) throws SQLException;

    protected abstract ResourceImpl load(Connection conn, String uri) throws SQLException;

    protected abstract void deleteExpiredLocks(Connection conn) throws SQLException;

    protected abstract void addChangeLogEntry(
        Connection conn, String loggerID, String loggerType,
        String uri, String operation, int resourceId,
        boolean collection, boolean recurse) throws SQLException;

    protected abstract String[] discoverLocks(Connection conn, ResourceImpl resource)
        throws SQLException;

    protected abstract String[] listSubTree(Connection conn, ResourceImpl parent)
        throws SQLException;

    protected abstract void store(Connection conn, ResourceImpl r)
        throws SQLException, IOException;

    protected abstract void delete(Connection conn, ResourceImpl resource)
        throws SQLException;

    protected abstract ResourceImpl[] loadChildren(Connection conn, ResourceImpl parent)
        throws SQLException;

    protected abstract String[] discoverACLs(Connection conn, ResourceImpl resource)
        throws SQLException;

    protected abstract void copy(Connection conn, ResourceImpl resource, String destURI,
                                 boolean copyACLs, boolean setOwner, String owner)
        throws SQLException, IOException;


}

