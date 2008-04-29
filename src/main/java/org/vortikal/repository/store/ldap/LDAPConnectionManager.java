/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.repository.store.ldap;

import netscape.ldap.LDAPCache;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPv3;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * Very simple LDAP connection manager.
 * Currently only supports anonymous and unsecured connections.
 * 
 * TODO: Should merge with UiOLDAPConnectionPool .. 
 *
 */
public class LDAPConnectionManager implements InitializingBean, DisposableBean {

    private static final Log LOG = LogFactory.getLog(LDAPConnectionManager.class);
    
    private int cacheSize = 1000000;
    private int cacheItemTimeout = 3600; 
    private int numConnections = 3;
    private int responseTimeLimit = 3; // Keep server reply timeout short
    private int nextConnection = 0;
    private String host;
    private int port = LDAPv3.DEFAULT_PORT;

    private LDAPCache cache;
    private LDAPConnection[] connections;
    
    public void afterPropertiesSet() {
        this.cache = new LDAPCache(this.cacheSize, this.cacheItemTimeout);
        this.connections = new LDAPConnection[this.numConnections];

        try {
            for (int i=0; i<this.numConnections; i++) {
                this.connections[i] = getNewConnectionInternal();
                LOG.debug("Initialized LDAP connection: " + this.connections[i]);
            }
        } catch (LDAPException e) {
            LOG.warn("Got an LDAPException while initializing connections: " 
                    + e.getLDAPErrorMessage());
        }
    }
    
    // Simple round-robin connection supply, and that shold work fine, 
    // since LDAPConnection is internally thread safe, as long as 
    // per-instance search constraints and other such state is not altered during usage.
    // An LDAPConnection instance also re-connects to server internally as needed 
    // (I have checked in the source code).
    public LDAPConnection getConnection() throws LDAPException {
        LDAPConnection conn = null;
        synchronized (this) {
            conn = this.connections[this.nextConnection];
            this.nextConnection = (this.nextConnection + 1) % this.numConnections;
        }
        
        if (conn != null) {
            return conn;
        } else { 
            LOG.warn("Encountered uninitialized connection in pool");
            throw new LDAPException("LDAPConnectionManager: no connection available");
        }
    }
    
    private LDAPConnection getNewConnectionInternal() throws LDAPException {
        LDAPConnection conn = new LDAPConnection();
        conn.setOption(LDAPConnection.PROTOCOL_VERSION, new Integer(3));
        conn.connect(this.host, this.port);
        conn.authenticate(null, null);
        LDAPSearchConstraints constraints = conn.getSearchConstraints();
        constraints.setServerTimeLimit(this.responseTimeLimit);
        conn.setSearchConstraints(constraints);
        if (this.cache != null) {
            conn.setCache(this.cache);
        }
        
        return conn;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setCacheItemTimeout(int cacheItemTimeout) {
        this.cacheItemTimeout = cacheItemTimeout;
    }

    public void setNumConnections(int numConnections) {
        this.numConnections = numConnections;
    }

    @Required
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setResponseTimeLimit(int responseTimeLimit) {
        this.responseTimeLimit = responseTimeLimit;
    }
    
    public void destroy() throws Exception {
        synchronized(this) {
            for (int i=0; i<this.numConnections; i++) {
                try {
                    LDAPConnection conn = this.connections[i];
                    if (conn != null && conn.isConnected()) {
                        conn.disconnect();
                    }
                } catch (LDAPException e) {
                    LOG.warn("Failed to disconnect an LDAP connection on destroy: " 
                                + e.getMessage());
                }
            }
        }
    }

}
