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
package org.vortikal.shell.vshell;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.vortikal.context.BaseContext;
import org.vortikal.repository.Path;
import org.vortikal.security.SecurityContext;
import org.vortikal.shell.AbstractConsole;

public class VShell extends AbstractConsole {

    private List<VCommand> commands = null;
    private Set<PathNode> toplevelNodes = new HashSet<PathNode>();
    private VShellContext context = new VShellContext();
    private SecurityContext securityContext;
    
    public void setCommands(List<VCommand> commands) {
        this.commands = commands;
    }
    
    public void setSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public void bind(String name, Object o) {
        this.context.set(name, o);
    }

    protected void evalInitFile(InputStream inStream, PrintStream out) {
    }

    
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void init() {
        if (this.commands == null) {
            Map m = BeanFactoryUtils.beansOfTypeIncludingAncestors(
                    getApplicationContext(), VCommand.class, false, true);
            this.commands = new ArrayList<VCommand>();
            this.commands.addAll(m.values());
        }
        
        addCommand(new HelpCommand());
        addCommand(new QuitCommand());
        addCommands();
    }
    
    public void eval(String line, PrintStream out) {
        if ("".equals(line.trim())) {
            return;
        }
        List<String> tokens = tokenize(line);
        Set<PathNode> ctx = this.toplevelNodes;
 
        List<PathNode> result = new ArrayList<PathNode>();
        int n = findCommand(result, ctx, tokens, 0);
        if (n == -1) {
            incompleteCommand(result, out);
            return;
        }
        
        CommandNode commandNode = (CommandNode) result.get(result.size() - 1);
        Map<String, Object> args = populateArgs(commandNode, tokens.subList(result.size(), tokens.size()), out);
        if (args == null) {
            out.println("Usage: " + commandNode.getCommand().getUsage());
            return;
        }
        try {
            if (this.securityContext != null) {
                BaseContext.pushContext();
                SecurityContext.setSecurityContext(this.securityContext);
            }
            commandNode.getCommand().execute(this.context, args, out);
        } catch (Throwable t) {
          out.println("Evaluation error: " + t.getMessage());
          t.printStackTrace(out);
            
        } finally {
            if (this.securityContext != null) {
                SecurityContext.setSecurityContext(null);
                BaseContext.popContext();
            }
        }
    }


    private void addCommands() {
        for (VCommand c : this.commands) {
            addCommand(c);
        }
    }
    
    private void addCommand(VCommand c) {
        
        String usage = c.getUsage();
        List<String> tokens = tokenize(usage);
        List<PathNode> parsedTokens = new ArrayList<PathNode>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            if ((token.startsWith("[<") && token.endsWith(">]")) 
                    || (token.startsWith("<") && token.endsWith(">"))) {
                ParamNode p = parseParamNode(token);
                PathNode last = parsedTokens.get(parsedTokens.size() - 1);
                if (!(last instanceof CommandNode)) {
                    throw new RuntimeException("Cannot add a parameter to an intermediate node");
                }
                ((CommandNode) last).addParamNode(p);
            } else if (i < tokens.size() - 1 && !tokens.get(i + 1).contains("<")){
                // Intermediate path node
                parsedTokens.add(new PathNode(token));
            } else {
                // "Leaf" command node
                parsedTokens.add(new CommandNode(token, c));
            }
        }
        Set<PathNode> ctx = toplevelNodes;

        for (PathNode n : parsedTokens) {
            PathNode existing = null;
            for (PathNode n2 : ctx) {
                if (n2.getName().equals(n.getName())) {
                    existing = n2;
                }
            }
            if (existing != null) {
                existing.children.addAll(n.children);
                ctx = existing.children;
            } else {
                ctx.add(n);
                ctx = n.children;
            }
        }
    }
    
    private ParamNode parseParamNode(String str) {
        boolean optional = false;
        if (str.startsWith("[<") && str.endsWith(">]")) {
            optional = true;
            str = str.substring(2, str.length() - 2);
        } else if (str.startsWith("<") && str.endsWith(">")) {
            str = str.substring(1, str.length() - 1);
            
        } else {
            throw new IllegalArgumentException("Invalid parameter: " + str);
        }
        int colIdx = str.indexOf(':');
        if (colIdx == -1) throw new RuntimeException("Missing ':' separator");
        String name = str.substring(0, colIdx);
        String type = str.substring(colIdx + 1, str.length());
        boolean rest = false;
        if (type.endsWith("...")) {
            type = type.substring(0, type.length() - 4);
            rest = true;
        }
        return new ParamNode(name.trim(), type.trim(), optional, rest);
    }
    

    private void incompleteCommand(List<PathNode> result, PrintStream out) {
        out.print("Incomplete command: ");
        for (PathNode node: result) {
            out.print(node.getName() + " ");
        }
        out.println();
        Set<PathNode> last = result.isEmpty() ? 
                this.toplevelNodes : 
                    result.get(result.size() - 1).children;
        
        if (last.size() == 1) {
            out.print("Expected '" + last.iterator().next().getName() + "'");
        } else {
            out.print("Expected one of: ");
            for (PathNode node : last) {
                out.print("'" + node.getName() + "' ");
            }
        }
        out.println();
    }

    
    private Map<String, Object> populateArgs(CommandNode commandNode, List<String> args, PrintStream out) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<ParamNode> argList = commandNode.getParamNodes();
        int paramIdx = 0;
        for (ParamNode arg : argList) {
            if (paramIdx == args.size()) {
                if (!arg.isOptional()) {
                    out.println("Error: missing argument '" + arg.name + "'");
                    return null;
                }
                result.put(arg.name, null);
                break;
            }
            if (arg.isRest()) {
                List<Object> multiple = new ArrayList<Object>();
                for (int i = paramIdx; i < args.size(); i++) {
                    try {
                        multiple.add(getTypedArg(args.get(i), arg.getType()));
                    } catch (Throwable t) {
                        out.println("Illegal value of argument: " + arg.name + ": " + t.getMessage());
                        return null;
                    }
                }
                result.put(arg.name, multiple);
                return result;
            }
            try {
                Object val = getTypedArg(args.get(paramIdx++), arg.getType());
                result.put(arg.name, val);
            } catch (Throwable t) {
                out.println("Illegal value of argument: " + arg.name + ": " + t.getMessage());
                return null;
            }
        }
        return result;
    }
    
    private Object getTypedArg(String val, String type) {
        if ("path".equals(type)) {
            return Path.fromString(val);
        }
        if ("number".equals(type)) {
            return Integer.parseInt(val);
        }
        return val;
    }
    
    private int findCommand(List<PathNode> result, Set<PathNode> nodes, List<String> tokens, int idx) {
        for (PathNode node : nodes) {
            if (idx >= tokens.size()) return -1;
            if (node.getName().equals(tokens.get(idx))) {
                result.add(node);
                if (node instanceof CommandNode) {
                    return idx;
                }
                int n = findCommand(result, node.children, tokens, idx + 1);  
                if (n != -1) return n;
            }
        }
        return -1;
    }
    

    private List<String> tokenize(String command) {
        List<String> result = new ArrayList<String>();
        StreamTokenizer st = new StreamTokenizer(new StringReader(command));
        st.resetSyntax();
        st.wordChars(0, 255);
        st.whitespaceChars(0, ' ');
        st.quoteChar('"');
        st.quoteChar('\'');
        int i = 0;
        try {
            boolean done = false;
            while (!done) {
                i = st.nextToken();
                switch (i) {
                case StreamTokenizer.TT_EOF:
                case StreamTokenizer.TT_EOL:
                    done = true;
                    break;
                default:
                    result.add(st.sval);
                    break;
                }
            }
        } catch (IOException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        return result;
    }

    private class QuitCommand implements VCommand {
        public String getDescription() {
            return "Quit VShell session";
        }
        
        public String getUsage() {
            return "quit";
        }
        
        public void execute(VShellContext c, Map<String, Object> args,
                PrintStream out) {
            out.println("Goodbye.");
            out.flush();
            Thread.currentThread().interrupt();
        }
    }
    
    private class HelpCommand implements VCommand {
        public String getDescription() {
            return "List available commands";
        }

        public String getUsage() {
            return "help [<command:string...>]";
        }

        public void execute(VShellContext c, Map<String, Object> args,
                PrintStream out) {

            @SuppressWarnings("unchecked")
            List<String> commands = (List<String>) args.get("command");
            
            if (commands != null) {
                List<PathNode> result = new ArrayList<PathNode>();
                findCommand(result, toplevelNodes, commands, 0);

                if (!result.isEmpty()) {
                    PathNode last = result.get(result.size() - 1);
                    
                    if (last instanceof CommandNode) {
                        printDescription((CommandNode) last, out);
                    } else {
                        out.println("Possible alternatives:");
                        printOptions(last, out);
                    }
                    return;
                }
            }
            out.println("Possible alternatives:");
            for (PathNode pathNode : toplevelNodes) {
                out.println(pathNode.getName());
            }
        } 
        
        private void printDescription(CommandNode commandNode, PrintStream out) {
            try {
                out.print(commandNode.getCommand().getUsage());
                out.println(": " + commandNode.getCommand().getDescription());
            } catch (Throwable t) {
                out.print("Error displaying help for command " + commandNode.getName());
                out.println(": " + t.getMessage());
            }
        }
        
        private void printOptions(PathNode node, PrintStream out) {
            for (PathNode child : node.children) {
                out.println(child.getName());
            }
        }

    }

    
    private class PathNode {

        private String name;
        private Set<PathNode> children = new HashSet<PathNode>();
        
        public PathNode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    private class CommandNode extends PathNode {

        private VCommand command;
        private List<ParamNode> paramNodes = new ArrayList<ParamNode>();
        
        public CommandNode(String name, VCommand command) {
            super(name);
            this.command = command;
        }

        public VCommand getCommand() {
            return this.command;
        }
        
        public void addParamNode(ParamNode node) {
            this.paramNodes.add(node);
        }
        
        public List<ParamNode> getParamNodes() {
            return this.paramNodes;
        }
    }
    
    private class ParamNode {
        private String name;
        private String type = "string";
        private boolean rest = false;
        private boolean optional = false;
        
        public ParamNode(String name, String type, boolean optional, boolean rest) {
            this.name = name;
            this.type = type;
            this.optional = optional;
            this.rest = rest;
        }
        
        public String getType() {
            return this.type;
        }
        
        public boolean isOptional() {
            return this.optional;
        }

        public boolean isRest() {
            return this.rest;
        }
    }
}
