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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class NodePathImpl implements NodePath {

    private List<Node> nodes;

    public NodePathImpl(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Node getNode() {
        return this.nodes.get(this.nodes.size() - 1);
    }

    public Node getParentNode() {
        if (this.nodes.size() > 1) {
            return this.nodes.get(this.nodes.size() - 2);
        }
        return null;
    }

    public NodePath extend(Node node) {
        List<Node> l = new ArrayList<Node>(this.nodes);
        l.add(node);
        return new NodePathImpl(l);
    }

    public NodePath getParentNodePath() {
        if (this.nodes.size() == 1) {
            return null;
        }
        List<Node> l = this.nodes.subList(0, this.nodes.size() - 1);
        return new NodePathImpl(l);
    }

    public Iterator<Node> fromRoot() {
        List<Node> list = new ArrayList<Node>(this.nodes);
        return list.iterator();
    }

    public Iterator<Node> towardsRoot() {
        List<Node> reversed = new ArrayList<Node>(this.nodes);
        Collections.reverse(reversed);
        return reversed.iterator();
    }

    public String toString() {
        return this.nodes.toString();
    }

}
