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
package org.vortikal.resourcemanagement.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.CommonTree;
import org.vortikal.repository.resource.ResourcetreeLexer;
import org.vortikal.resourcemanagement.ScriptDefinition;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.ScriptDefinition.ScriptType;

public class ScriptDefinitionParser {

    @SuppressWarnings("unchecked")
    public void parseScripts(StructuredResourceDescription srd, List<CommonTree> children) {
        for (CommonTree scriptEntry : children) {
            String propName = scriptEntry.getText();
            CommonTree scriptType = (CommonTree) scriptEntry.getChild(0);
            switch (scriptType.getType()) {
            case ResourcetreeLexer.SHOWHIDE:
                srd.addScriptDefinition(parseScriptDefinition(propName, ScriptType.SHOWHIDE, scriptType.getChildren()));
                break;
            case ResourcetreeLexer.MULTIPLEINPUTFIELDS:
                srd.addScriptDefinition(new ScriptDefinition(propName, ScriptType.MULTIPLEINPUTFIELDS, null));
            default:
                break;
            }
        }
    }

    private ScriptDefinition parseScriptDefinition(String propName, ScriptType scriptType, List<CommonTree> paramValues) {
        Object params = getScriptParams(scriptType, paramValues);
        ScriptDefinition sd = new ScriptDefinition(propName, scriptType, params);
        return sd;
    }

    private Object getScriptParams(ScriptType scriptType, List<CommonTree> paramValues) {
        if (ScriptType.AUTOCOMPLETE.equals(scriptType)) {
            return getAutoCompleteParams(paramValues);
        } else if (ScriptType.SHOWHIDE.equals(scriptType)) {
            return getShowHideParams(paramValues);
        }
        return null;
    }

    private Object getAutoCompleteParams(List<CommonTree> paramValues) {
        Map<String, String> params = new HashMap<String, String>();
        for (CommonTree param : paramValues) {
            params.put(param.getText(), param.getChild(0).getText());
        }
        return params;
    }

    @SuppressWarnings("unchecked")
    private Object getShowHideParams(List<CommonTree> paramValues) {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        for (CommonTree param : paramValues) {
            String trigger = param.getText();
            List<String> affectedProps = new ArrayList<String>();
            List<CommonTree> l = param.getChildren();
            for (CommonTree c : l) {
                affectedProps.add(c.getText());
            }
            params.put(trigger, affectedProps);
        }
        return params;
    }

}
