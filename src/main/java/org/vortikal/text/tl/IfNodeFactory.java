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
package org.vortikal.text.tl;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public class IfNodeFactory implements DirectiveNodeFactory {

    private static final Set<String> IF_ELSE_TERM =
        new HashSet<String>(Arrays.asList("else", "endif"));

    private static final Set<String> IF_TERM =
        new HashSet<String>(Arrays.asList("endif"));


    private Map<Symbol, Operator> operators 
        = new HashMap<Symbol, Operator>();
    
    public static enum Presedence {
        ONE(1),
        TWO(2),
        THREE(3);
        private int n;
        private Presedence(int n) {
            this.n = n;
        }
        public int value() {
            return n;
        }
    }
    public interface Operator {
        public Symbol getSymbol();
        public Presedence getPresedence();
        public Object eval(Stack<Object> stack);
    }
    
    public void addOperator(Operator operator) {
        this.operators.put(operator.getSymbol(), operator);
    }
    
    private static final Symbol EQ = new Symbol("=");
    private static final Symbol NEQ = new Symbol("!=");
    private static final Symbol AND = new Symbol("&&");
    private static final Symbol OR = new Symbol("||");
    private static final Symbol NOT = new Symbol("!");

    public IfNodeFactory() {
        addOperator(new Eq(EQ, Presedence.THREE));
        addOperator(new Neq(NEQ, Presedence.THREE));
        addOperator(new And(AND, Presedence.ONE));
        addOperator(new Or(OR, Presedence.ONE));
        addOperator(new Not(NOT, Presedence.THREE));
    }
    
    public Node create(DirectiveParseContext ctx) throws Exception {
        
        List<Argument> args = ctx.getArguments();
        if (args.isEmpty()) {
            throw new RuntimeException("Missing condition: "
                    + ctx.getNodeText());
        }
        
        Stack<Symbol> stack = new Stack<Symbol>();
        List<Argument> result = new ArrayList<Argument>();

        for (Argument arg : args) {
            if (arg instanceof Literal) {
                result.add(arg);
                continue;
            } 
            Symbol symbol = (Symbol) arg;
            
            if ("(".equals(symbol.getSymbol())) {
                stack.push(symbol);
                continue;
            } 

            if (")".equals(symbol.getSymbol())) {
                while (true) {
                    if (stack.isEmpty()) {
                        throw new RuntimeException("Unbalanced parenthesis");
                    }
                    Symbol top = stack.pop();
                    if ("(".equals(top.getSymbol())) {
                        break;
                    }
                    result.add(top);
                }
                continue;
            }
            
            Operator op = this.operators.get(symbol);
            if (op == null) {
                result.add(symbol);
                continue;
            }
            
            if (stack.isEmpty()) {
                stack.push(symbol);
                continue;
            }
            Operator top = this.operators.get(stack.peek());
            int n = op.getPresedence().value();
            while (top != null && top.getPresedence().value() >= n) {
                result.add(top.getSymbol());
                stack.pop();
                if (stack.isEmpty()) {
                    top = null;
                } else {
                    top = this.operators.get(stack.peek());
                }
            }
            stack.push(symbol);
        }
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        ParseResult trueBranch = ctx.getParser().parse(IF_ELSE_TERM);
        ParseResult falseBranch = null;

        String terminator = trueBranch.getTerminator();
        if (terminator == null) {
            throw new RuntimeException("Unterminated directive: " + ctx.getName());
        }
        if (terminator.equals("else")) {
            falseBranch = ctx.getParser().parse(IF_TERM);
            terminator = falseBranch.getTerminator();
            if (terminator == null) {
                throw new RuntimeException("Unterminated directive: " + ctx.getNodeText());
            }
        }
        NodeList trueNodeList = trueBranch.getNodeList();
        NodeList falseNodeList = falseBranch != null ? falseBranch.getNodeList() : null;
        return new IfNode(result, trueNodeList, falseNodeList);
    }


    private class IfNode extends Node {
        private List<Argument> expression;
        private NodeList trueBranch;
        private NodeList falseBranch;

        public IfNode(List<Argument> expression, NodeList trueBranch, NodeList falseBranch) {
            this.expression = expression;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }

        public void render(Context ctx, Writer out) throws Exception {

            boolean result;
            Object o = eval(ctx);
            if (o == null) {
                result = false;
            } else if (o instanceof Boolean) {
                result = ((Boolean) o).booleanValue();
            } else {
                result = true;
            }
            
            ctx.push();
            if (result) {
                this.trueBranch.render(ctx, out);

            } else if (this.falseBranch != null) {
                this.falseBranch.render(ctx, out);
            }
            ctx.pop();
        }
        
        private Object eval(Context ctx) {
            Stack<Object> stack = new Stack<Object>();
            for (Argument arg : this.expression) {
                if (arg instanceof Literal) {
                    stack.push(((Literal) arg).getValue());
                } else {
                    Symbol s = (Symbol) arg;
                    Operator op = operators.get(s);
                    if (op == null) {
                        stack.push(s.resolve(ctx));
                    } else {
                        stack.push(op.eval(stack));
                    }
                }
            }
            if (stack.size() != 1) {
                throw new RuntimeException("Unable to evaluate expression");
            }
            return stack.peek();
        }

        public String toString() {
            return "[if-node]";
        }
    }

    private abstract class InternalOperator implements Operator {
        private Symbol symbol;
        private Presedence presedence;

        public InternalOperator(Symbol symbol, Presedence presedence) {
            this.symbol = symbol;
            this.presedence = presedence;
        }
        public Symbol getSymbol() {
            return this.symbol;
        }
        public Presedence getPresedence() {
            return this.presedence;
        }
        public String toString() {
            return this.symbol.getSymbol();
        }
        public abstract Object eval(Stack<Object> stack);
    }
    
    private class Eq extends InternalOperator {
        public Eq(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }
        public Object eval(Stack<Object> stack) {
            Object o1 = stack.pop();
            Object o2 = stack.pop();
            if (o1 == null && o2 == null) {
                return true;
            } else if (o1 == null && o2 != null) {
                return false;
            } else if (o1 != null && o2 == null) {
                return false;
            }
            return Boolean.valueOf(o1.equals(o2));
        }
    }

    private class Neq extends InternalOperator {
        public Neq(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }
        public Object eval(Stack<Object> stack) {
            Object o1 = stack.pop();
            Object o2 = stack.pop();
            if (o1 == null && o2 == null) {
                return false;
            } else if (o1 == null && o2 != null) {
                return true;
            } else if (o1 != null && o2 == null) {
                return true;
            }
            return Boolean.valueOf(!o1.equals(o2));
        }
    }

    private class And extends InternalOperator {
        public And(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }
        public Object eval(Stack<Object> stack) {
            Object o1 = stack.pop();
            Object o2 = stack.pop();
            if (o1 == null || o2 == null) {
                return false;
            }
            if (o1 instanceof Boolean) {
                if (!(Boolean) o1) {
                    return false;
                }
            }
            if (o2 instanceof Boolean) {
                if (!(Boolean) o2) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private class Or extends InternalOperator {
        public Or(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }
        public Object eval(Stack<Object> stack) {
            Object o1 = stack.pop();
            Object o2 = stack.pop();
            if (o1 != null) {
                if (!(o1 instanceof Boolean)) {
                    return true;
                }
                if ((Boolean) o1) {
                    return true;
                }
            }
            if (o2 != null) {
                if (!(o2 instanceof Boolean)) {
                    return true;
                }
                if ((Boolean) o2) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private class Not extends InternalOperator {
        public Not(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }
        public Object eval(Stack<Object> stack) {
            Object o = stack.pop();
            if (o == null) {
                return true;
            }
            if (o instanceof Boolean) {
                return !((Boolean) o);
            }
            return false;
        }
    }

}
