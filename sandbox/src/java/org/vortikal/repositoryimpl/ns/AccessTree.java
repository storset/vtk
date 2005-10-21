/* Copyright (c) 2005, University of Oslo, Norway
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
package org.vortikal.repositoryimpl.ns;

public class AccessTree {

    private AccessNode rootNode;
    private Class accessObjectClass;
    
    public AccessTree() {
        this(null);
    }

    public AccessTree(Class accessObjectClass) {
        this.accessObjectClass = accessObjectClass;
        this.rootNode = new AccessNode("/", null, getAccessObject());
    }
    

    public final AccessNode getAccess(String[] path) {
        AccessNode node = this.rootNode;

        for (int i = 1; i <= path.length; i++) {
            synchronized(node) {
                if (i < path.length) {
                    AccessNode child = node.getChild(path[i]);
                    if (child == null) {
                        child = new AccessNode(path[i], node, getAccessObject());
                        node.addChild(path[i], child);
                    }
                    node.incrementChildAccessors(path[i]);
                    node = node.getChild(path[i]);
                }
            }
        }
        return node;
    }


    public final void releaseAccess(AccessNode node) {
        AccessNode child = null;

        while (node != null) {
            synchronized(node) {
                
                if (child != null) {
                    node.decrementChildAccessors(child.getName());
                    if (node.getAccessors(child.getName())  == 0) {
                        node.removeChild(child.getName());
                    } 
                }
                child = node;
                node = node.getParent();
            }
        }
    }
    
    
    public final AccessNode getRootNode() {
        return this.rootNode;
    }
    

    protected Object getAccessObject() {

        if (this.accessObjectClass != null) {
            try {
                return this.accessObjectClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new Object();
    }
    


    public String toString() {
        StringBuffer sb = new StringBuffer();
        getTree(this.rootNode, 0, sb, "  ");
        return sb.toString();
    }
    

    private void getTree(AccessNode node, int accessors, StringBuffer sb, String indent) {

        sb.append(indent).append(node);
        sb.append("\n");

        for (java.util.Iterator i = node.getChildren(); i.hasNext();) {
            AccessNode child = (AccessNode) i.next();
            getTree(child, node.getAccessors(child.getName()), sb, indent + "  ");
        }
    }
}
