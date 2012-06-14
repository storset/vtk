/* Copyright (c) 2012, University of Oslo, Norway
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

package org.vortikal.repository.store.db.ibatis;

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.jdbc.support.lob.OracleLobHandler;

/**
 * Factory bean for {@link LobHandler} implementation based on SQL dialect in use.
 */
public class LobHandlerFactoryBean implements FactoryBean<LobHandler>, InitializingBean {

    private String sqlDialect;
    private LobHandler lobHandler;
    private Log logger = LogFactory.getLog(getClass());
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if ("oracle".equals(this.sqlDialect)) {
            logger.info("--- Using OracleLobHandler");
            this.lobHandler = new OracleLobHandler();
        } else if ("postgresql".equals(this.sqlDialect)) {
            logger.info("--- Using PGLobHandler");
            this.lobHandler = new PGLobHandler();
        } else {
            logger.info("--- Using DefaultLobHandler");
            this.lobHandler = new DefaultLobHandler();
        }
    }
    
    @Override
    public LobHandler getObject() throws Exception {
        if (this.lobHandler == null) {
            throw new FactoryBeanNotInitializedException();
        }
        logger.info("--- getObject() init ok");
        return this.lobHandler;
    }

    @Override
    public Class<?> getObjectType() {
        return LobHandler.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
    
    public void setSqlDialect(String sqlDialect) {
        this.sqlDialect = sqlDialect;
    }
    
}

/**
 * Specialized LobHandler for PostgreSQL with JDBC4.
 * - Clob/text data is never wrapped as Clob.
 * - Blob/binary data is always wrapped as Blob.
 * 
 */
class PGLobHandler extends DefaultLobHandler {

    public PGLobHandler() {
        // Needed for Blobs to work properly with PostgreSQL JDBC driver
        super.setWrapAsLob(true);
    }

    @Override
    public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getAsciiStream(columnIndex);
    }

    @Override
    public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getCharacterStream(columnIndex);
    }

    @Override
    public LobCreator getLobCreator() {
        return new PGLobHandler.PGLobCreator();
    }
    
    protected class PGLobCreator extends DefaultLobHandler.DefaultLobCreator {
        @Override
        public void setClobAsString(PreparedStatement ps, int paramIndex, String content) throws SQLException {
            ps.setString(paramIndex, content);
        }

        @Override
        public void setClobAsAsciiStream(PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength) throws SQLException {
            ps.setAsciiStream(paramIndex, asciiStream, contentLength);
        }

        @Override
        public void setClobAsCharacterStream(PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength) throws SQLException {
            ps.setCharacterStream(paramIndex, characterStream, contentLength);
        }
    }
    
    @Override
    public void setWrapAsLob(boolean wrapAsLob) {
        throw new UnsupportedOperationException("wrapAsLob cannot be configured");
    }

    @Override
    public void setStreamAsLob(boolean streamAsLob) {
        throw new UnsupportedOperationException("streamAsLob cannot be configured");
    }
}

