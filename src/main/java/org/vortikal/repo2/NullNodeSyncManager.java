/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repo2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NullNodeSyncManager implements NodeSyncManager {

    private Map<NodeID, NodeSyncToken> tokenList = new ConcurrentHashMap<NodeID, NodeSyncToken>();

    public NodeSyncToken lock(Node... nodes) {
        NodeSyncToken token = null;
        for (Node node : nodes) {
            // Get a token for the node
            token = getToken(node.getNodeID());
            // Attempt to aquire a lock
            token.aquire();            
        }
        return token;
    }

    public void unlock(NodeSyncToken token) {
        token.release();
        if (token.getCount() == 0) {
            disposeToken(token);
        }
    }

    private synchronized NodeSyncToken getToken(NodeID nodeID) {
        if (tokenList.containsKey(nodeID)) {
            return tokenList.get(nodeID);
        }
        NodeSyncToken token = new NullNodeSyncToken();
        this.tokenList.put(nodeID, token);
        return token;
    }

    private synchronized void disposeToken(NodeSyncToken token) {
        tokenList.remove(token);
    }

    private class NullNodeSyncToken implements NodeSyncToken {

        // Number of nodes held by this token
        private int count;

        public NullNodeSyncToken() {
            // NodeID ?
            // Thread ?
        }

        public synchronized void aquire() {
            // XXX IMPLEMENT!!!
            if (true) {
                // We've aquired a lock
                this.count++;
            } else {
                // If we can't get a lock, wait
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public synchronized void release() {
            // Release the lock
            this.count--;
            notifyAll();
        }

        public synchronized int getCount() {
            return this.count;
        }

    }

}
