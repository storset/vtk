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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class AccessNode {

    private String name;
    private AccessNode parent;
    private Map children = new HashMap();
    private Map childAccessors = new HashMap();
    private Object accessObject;
    

    public AccessNode(String name, AccessNode parent, Object accessObject) {
        this.name = name;
        this.parent = parent;
        this.accessObject = accessObject;
    }
        

    public String getName() {
        return this.name;
    }


    public AccessNode getParent() {
        return this.parent;
    }


    public boolean isRootNode() {
        return this.parent == null;
    }
    

    public Object getAccessObject() {
        return this.accessObject;
    }
    

    public AccessNode getChild(String childName) {
        return (AccessNode) this.children.get(childName);
    }

        
    public Iterator getChildren() {
        return this.children.values().iterator();
    }
    

    public void addChild(String childName, AccessNode child) {
        this.children.put(childName, child);
        this.childAccessors.put(childName, new Integer(0));
    }

        
    public int getAccessors(String childName) {
        Integer accessors = (Integer) this.childAccessors.get(childName);
        if (accessors == null) {
            throw new IllegalArgumentException(
                "Child " + childName + " does not exist on node " + this);
        }
        return accessors.intValue();
    }

        
    public void decrementChildAccessors(String childName) {
        Integer accessors = (Integer) this.childAccessors.get(childName);
        if (accessors == null) {
            throw new IllegalArgumentException(
                "Child " + childName + " does not exist on node " + this);
        }
        accessors = new Integer(accessors.intValue() - 1);
        this.childAccessors.put(childName, accessors);
    }


    public void incrementChildAccessors(String childName) {
        Integer accessors = (Integer) this.childAccessors.get(childName);
        if (accessors == null) {
            throw new IllegalArgumentException(
                "Child " + childName + " does not exist on node " + this);
        }
        accessors = new Integer(accessors.intValue() + 1);
        this.childAccessors.put(childName, accessors);
    }


    public void removeChild(String childName) {
        this.children.remove(childName);
        this.childAccessors.remove(childName);
    }
        

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.name).append(" [").append(this.accessObject).append("]");
        return sb.toString();
    }
    
}
