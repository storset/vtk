/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.vortikal.repository.HierarchicalVocabulary;

public class HierarchicalValueVocabulary implements HierarchicalVocabulary<Value>, InitializingBean {

    private PropertyType.Type type = PropertyType.Type.STRING;
    private String messageSourceBaseName;
    private List<HierarchicalNode<Value>> nodes;
    private Map<Value, List<Value>> descendantsAndSelfMap = new HashMap<Value, List<Value>>();
    private Value[] allowedValues;
    private ValueFormatter valueFormatter;
    
    public List<Value> getResourceTypeDescendants(Value entry) {
        return this.descendantsAndSelfMap.get(entry);
    }

    public Value[] getAllowedValues() {
        return this.allowedValues;
    }

    public List<HierarchicalNode<Value>> getRootNodes() {
        return this.nodes;
    }
    
    public ValueFormatter getValueFormatter() {
        return this.valueFormatter;
    }

    public void setMessageSourceBaseName(String messageSourceBaseName) {
        this.messageSourceBaseName = messageSourceBaseName;
    }

    public void setNodes(List<HierarchicalNode<Value>> nodes) {
        this.nodes = nodes;
    }

    public void setStringNodes(List<HierarchicalNode<String>> stringNodes) {
        if (stringNodes == null)
            return;
            
        this.nodes = new ArrayList<HierarchicalNode<Value>>();
            
        for (HierarchicalNode<String> stringNode : stringNodes) {
            this.nodes.add(buildNode(stringNode));
        }
    }

    public void setType(PropertyType.Type type) {
        this.type = type;
    }

    private HierarchicalNode<Value> buildNode(HierarchicalNode<String> stringNode) {
        HierarchicalNode<Value> node = new HierarchicalNode<Value>();
        node.setEntry(new Value(stringNode.getEntry(), this.type));

        List<HierarchicalNode<String>> stringChildren = stringNode.getChildren();
        
        if (stringChildren != null) {
            List<HierarchicalNode<Value>> children = new ArrayList<HierarchicalNode<Value>>();
        
            for (HierarchicalNode<String> stringChild : stringChildren) {
                children.add(buildNode(stringChild));
            }
            node.setChildren(children);
        }
        
        return node;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.nodes == null)
            return;
        
        for (HierarchicalNode<Value> node : this.nodes) {
            buildDescendants(node);
        }

        List<Value> values = new ArrayList<Value>();

        for (HierarchicalNode<Value> rootNode: this.nodes) {
            values.addAll(this.descendantsAndSelfMap.get(rootNode.getEntry()));
        }
        
        this.allowedValues = values.toArray(new Value[values.size()]);
        this.valueFormatter = new MessageSourceValueFormatter(messageSourceBaseName, this.type);
    }

    private List<Value> buildDescendants(HierarchicalNode<Value> node) {
        List<Value> descendantsAndSelf = new ArrayList<Value>();
        descendantsAndSelf.add(node.getEntry());

        List<HierarchicalNode<Value>> children = node.getChildren();

        if (children != null) {
            for (HierarchicalNode<Value> child : children) {
                descendantsAndSelf.addAll(buildDescendants(child));
            }
        }
        
        this.descendantsAndSelfMap.put(node.getEntry(), descendantsAndSelf);
        
        return descendantsAndSelf;
    }

}
