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
package org.vortikal.repositoryimpl.index.observation;

import java.util.Date;

/**
 * Superclass for all classes representing resource changes from Vortex resource
 * events.
 *
 * @author oyviste
 */
public abstract class ResourceChange {
    private String uri = null;
    private int id = -1; // Not resource ID, but change-event ID.
    private long timestamp = -1;
    private int loggerId = -1;
    private int loggerType = -1;
    private boolean collection;

    public ResourceChange() {}

    public ResourceChange(String uri, int id, long timestamp, 
                          int loggerId, int loggerType, boolean collection) {
        this.uri = uri;
        this.id = id;
        this.timestamp = timestamp;
        this.loggerId = loggerId;
        this.loggerType = loggerType;
    }


    public String getUri() {
        return this.uri;
    }

    public int getId() {
        return this.id;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getLoggerId() {
        return this.loggerId;
    }

    public int getLoggerType() {
        return this.loggerType;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setLoggerId(int loggerId) {
        this.loggerId = loggerId;
    }

    public void setLoggerType(int loggerType) {
        this.loggerType = loggerType;
    }

    public boolean isCollection() {

        return this.collection;
    }

    public void setCollection(boolean collection) {

        this.collection = collection;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.getClass().getName()).append(": ");
        sb.append("[uri: ").append(uri);
        sb.append(", id = ").append(this.id);
        sb.append(", loggerType = ").append(this.loggerType);
        sb.append(", loggerId = ").append(this.loggerId);
        sb.append(", timestamp = ").append(new Date(timestamp));
        sb.append(", collection = ").append(this.collection);
        sb.append("]");

        return sb.toString();
    }
}
