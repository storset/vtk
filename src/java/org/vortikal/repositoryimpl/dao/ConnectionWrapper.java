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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;

import java.util.Map;


public class ConnectionWrapper implements Connection {
    private Connection realConnection = null;
    private ConnectionManager manager = null;

    public ConnectionWrapper(Connection realConnection,
        ConnectionManager manager) {
        this.realConnection = realConnection;
        this.manager = manager;
    }

    public String nativeSQL(String sql) throws SQLException {
        return realConnection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        realConnection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return realConnection.getAutoCommit();
    }

    public String getCatalog() throws SQLException {
        return realConnection.getCatalog();
    }

    public void commit() throws SQLException {
        realConnection.commit();
    }

    public void rollback() throws SQLException {
        realConnection.rollback();
    }

    public void close() throws SQLException {
        manager.releaseConnection(this);
    }

    public boolean isClosed() throws SQLException {
        return realConnection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return realConnection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        realConnection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return realConnection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        realConnection.setCatalog(catalog);
    }

    public void setTransactionIsolation(int level) throws SQLException {
        realConnection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return realConnection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return realConnection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        realConnection.clearWarnings();
    }

    public Statement createStatement() throws SQLException {
        return realConnection.createStatement();
    }

    public PreparedStatement prepareStatement(String sql)
        throws SQLException {
        return realConnection.prepareStatement(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return realConnection.prepareCall(sql);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException {
        return realConnection.createStatement(resultSetType,
            resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
        return realConnection.prepareStatement(sql, resultSetType,
            resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency) throws SQLException {
        return realConnection.prepareCall(sql, resultSetType,
            resultSetConcurrency);
    }

    public Map getTypeMap() throws SQLException {
        return realConnection.getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        realConnection.setTypeMap(map);
    }

    public void setHoldability(int holdability) throws SQLException {
        realConnection.setHoldability(holdability);
    }

    public int getHoldability() throws SQLException {
        return realConnection.getHoldability();
    }

    public Savepoint setSavepoint() throws SQLException {
        return realConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return realConnection.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        realConnection.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        realConnection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return realConnection.createStatement(resultSetType,
            resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return realConnection.prepareStatement(sql, resultSetType,
            resultSetConcurrency, resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
        return realConnection.prepareCall(sql, resultSetType,
            resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        return realConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
        return realConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
        return realConnection.prepareStatement(sql, columnNames);
    }
}
