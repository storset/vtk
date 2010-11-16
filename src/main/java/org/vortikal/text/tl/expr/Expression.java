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
package org.vortikal.text.tl.expr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.vortikal.text.tl.Argument;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Literal;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.expr.Operator.Notation;
import org.vortikal.text.tl.expr.Operator.Precedence;

/**
 * Utility class for parsing and evaluating infix expressions (e.g.
 * <code>(x = y) || (x = z)</code>).
 */
public class Expression {

    private static final Symbol EMPTY_LIST = new Symbol("#emptylist");
    private static final Symbol EMPTY_MAP = new Symbol("#emptymap");
    private static final Symbol COLLECTION = new Symbol("#collection");
    
    private static final Symbol LP = new Symbol("(");
    private static final Symbol RP = new Symbol(")");
    private static final Symbol LCB = new Symbol("{");
    private static final Symbol RCB = new Symbol("}");
    private static final Symbol COMMA = new Symbol(",");

    private static final Symbol EQ = new Symbol("=");
    private static final Symbol NEQ = new Symbol("!=");
    private static final Symbol GT = new Symbol(">");
    private static final Symbol LT = new Symbol("<");
    private static final Symbol AND = new Symbol("&&");
    private static final Symbol OR = new Symbol("||");
    private static final Symbol NOT = new Symbol("!");
    private static final Symbol PLUS = new Symbol("+");
    private static final Symbol MINUS = new Symbol("-");
    private static final Symbol MULTIPLY = new Symbol("*");
    private static final Symbol DIVIDE = new Symbol("/");
    private static final Symbol MAPPING = new Symbol(":");
    private static final Symbol ACCESSOR = new Symbol(".");
    
    /**
     * The default set of operators
     */
    public static final Map<Symbol, Operator> DEFAULT_OPERATORS;
    static {
        Map<Symbol, Operator> ops = new HashMap<Symbol, Operator>();

        ops.put(ACCESSOR, new Accessor(ACCESSOR, Notation.INFIX, Precedence.TWELVE));

        ops.put(NOT, new Not(NOT, Notation.PREFIX, Precedence.ELEVEN));

        ops.put(DIVIDE, new Divide(DIVIDE, Notation.INFIX, Precedence.TEN));
        ops.put(MULTIPLY, new Multiply(MULTIPLY, Notation.INFIX, Precedence.NINE));

        ops.put(PLUS, new Plus(PLUS, Notation.INFIX, Precedence.EIGHT));
        ops.put(MINUS, new Minus(MINUS, Notation.INFIX, Precedence.SEVEN));

        ops.put(GT, new Gt(GT, Notation.INFIX, Precedence.SIX));
        ops.put(LT, new Lt(LT, Notation.INFIX, Precedence.FIVE));

        ops.put(EQ, new Eq(EQ, Notation.INFIX, Precedence.FOUR));
        ops.put(NEQ, new Neq(NEQ, Notation.INFIX, Precedence.THREE));

        ops.put(AND, new And(AND, Notation.INFIX, Precedence.TWO));
        ops.put(OR, new Or(OR, Notation.INFIX, Precedence.ONE));

        ops.put(MAPPING, new Mapping(MAPPING, Notation.INFIX, Precedence.ZERO));
        
        DEFAULT_OPERATORS = Collections.unmodifiableMap(new HashMap<Symbol, Operator>(ops));
    }

    /**
     * The set of defined operators for this expression
     */
    private Map<Symbol, Operator> operators = new HashMap<Symbol, Operator>(DEFAULT_OPERATORS);

    private Map<Symbol, Operator> functions = new HashMap<Symbol, Operator>();

    /**
     * The expression in its original infix notation
     */
    private List<Argument> infix;

    /**
     * The expression converted to postfix notation
     */
    private List<Object> postfix;

    public Expression(List<Argument> args) {
        this(null, args);
    }
    
    /**
     * Constructs an expression using a supplied set of functions
     */
    public Expression(Set<Function> functions, List<Argument> args) {

        if (functions != null) {
            for (Function f : functions) {
                addFunction(f);
            }
        }
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        this.infix = new ArrayList<Argument>(args);
        Stack<Symbol> stack = new Stack<Symbol>();
        List<Object> output = new ArrayList<Object>();

        Argument prev = null, next = null;
        for (int i = 0; i < args.size(); i++) {
            Argument arg = args.get(i);
            if (i > 0) {
                prev = args.get(i - 1);
            }
            if (i < args.size() - 1) {
                next = args.get(i + 1);
            }
            if ((arg instanceof Literal) && (prev instanceof Literal)) {
                throw new RuntimeException("Malformed expression: " + this);
            }
            if (ACCESSOR.equals(arg) && prev == null) {
                throw new IllegalArgumentException("Malformed expression: " + this);
            }
            
            if (arg instanceof Literal) {
                output.add(arg);
                continue;
            }
            Symbol symbol = (Symbol) arg;

            if (LP.equals(symbol)) {
                stack.push(symbol);
                continue;
            }

            if (LCB.equals(symbol)) {
                stack.push(symbol);
                continue;
            }
            
            if (LP.equals(next)) {
                // Function: '<symbol> ('
                stack.push(symbol);
                continue;
            }
            
            if (RP.equals(symbol)) {
                if (LP.equals(prev)) {
                    // Special case: function call with no arguments: '<function>()'
                    if (stack.size() < 2) {
                        throw new RuntimeException("Malformed expression: " + this);
                    }
                    stack.pop();
                    Symbol top = stack.pop();
                    Operator op = this.operators.get(top);
                    if (op != null) {
                        stack.push(top);
                    } else {
                        int argCount = 0;
                        if (!stack.isEmpty() && ACCESSOR.equals(stack.peek())) {
                            stack.pop();
                            argCount++;
                        }
                        output.add(new Funcall(top, argCount));
                    }
                    continue;
                }
                int commas = 0;
                while (true) {
                    if (stack.isEmpty()) {
                        throw new RuntimeException("Unbalanced parentheses in expression " + this);
                    }
                    Symbol top = stack.pop();
                    if (LP.equals(top)) {
                        break;
                    }
                    if (COMMA.equals(top)) {
                        commas++;
                    } else {
                        output.add(top);
                    }
                }
                if (!stack.isEmpty()) {
                    Symbol top = stack.pop();
                    Operator op = this.operators.get(top);
                    if (op != null) {
                        stack.push(top);
                    } else {
                        int argCount = commas == 0 ? 1 : commas + 1;
                        if (!stack.isEmpty() && ACCESSOR.equals(stack.peek())) {
                            stack.pop();
                            argCount++;
                        }
                        output.add(new Funcall(top, argCount));
                    }
                }
                continue;
            }

            if (RCB.equals(symbol)) {
                if (LCB.equals(prev)) {
                    // Special case: empty list ('{}'):
                    stack.pop();
                    output.add(EMPTY_LIST);
                    continue;
                } else if (MAPPING.equals(prev)) {
                    if (stack.size() > 1 && LCB.equals(stack.elementAt(stack.size() - 2))) {
                        // Special case: empty map ('{:}')
                        stack.pop();
                        stack.pop();
                        output.add(EMPTY_MAP);
                        continue;
                    }
                }
                int commas = 0;
                while (true) {
                    if (stack.isEmpty()) {
                        throw new RuntimeException("Unbalanced map definition in expression " + this);
                    }
                    Symbol top = stack.pop();
                    if (LCB.equals(top)) {
                        break;
                    }
                    if (COMMA.equals(top)) {
                        commas++;
                    } else {
                        output.add(top);
                    }
                }
                output.add(new Literal(String.valueOf(commas + 1)));
                output.add(COLLECTION);
                continue;
            }
            
            if (COMMA.equals(symbol)) {
                while (true) {
                    if (stack.isEmpty()) {
                        throw new RuntimeException("Malformed expression: " + this);
                    }
                    Symbol top = stack.peek();
                    if (LP.equals(top) || LCB.equals(top) || COMMA.equals(top)) {
                        break;
                    }
                    top = stack.pop();
                    output.add(top);
                }
                stack.push(symbol);
                continue;
            }

            Operator op = this.operators.get(symbol);
            if (op == null) {
                output.add(symbol);
                continue;
            }

            if (stack.isEmpty()) {
                stack.push(symbol);
                continue;
            }
            Operator top = this.operators.get(stack.peek());
            int n = op.getprecedence().value();
            while (top != null && top.getprecedence().value() > n) {
                output.add(top.getSymbol());
                stack.pop();
                if (stack.isEmpty()) {
                    top = null;
                } else {
                    top = this.operators.get(stack.peek());
                }
            }
            // Associativity is considered only between equal operators.
            if (top != null && top.getSymbol().equals(symbol) && top.leftAssociative()) {
                output.add(top.getSymbol());
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
            Symbol top = stack.pop();
            if (LP.equals(top) || RP.equals(top) || COMMA.equals(top)) {
                throw new RuntimeException("Invalid expression: " + this);
            }
            output.add(top);
        }
        this.postfix = output;
    }

    /**
     * Evaluates the expression. Returns a single object value as the result.
     */
    public Object evaluate(Context ctx) {
        EvalStack stack = new EvalStack(ctx);
        try {
            for (Object o : this.postfix) {
                if (o instanceof Funcall) {
                    Funcall funcall = (Funcall) o;
                    Operator function = functions.get(funcall.name);
                    if (function == null) {
                        throw new RuntimeException("Error in expression " + infix 
                                + ": undefined function: " + funcall.name.getSymbol());
                    }
                    stack.push(funcall.args);
                    Object val = function.eval(ctx, stack);
                                        stack.push(val);
                } else {
                    Argument arg = (Argument) o;
                    if (arg instanceof Literal) {
                        stack.push(arg.getValue(ctx));
                    } else if (arg instanceof Funcall) {

                    } else {
                        Symbol s = (Symbol) arg;
                        if (EMPTY_MAP.equals(s)) {
                            stack.push(new HashMap<Object, Object>());
                        } else if (EMPTY_LIST.equals(s)) {
                            stack.push(new ArrayList<Object>());
                        } else if (COLLECTION.equals(s)) {
                            stack.push(defineCollection(stack));
                        } else {
                            Operator op = operators.get(s);
                            if (op == null) {
                                // Symbol/variable:
                                stack.push(s);
                            } else {
                                // Operator:
                                Object val = op.eval(ctx, stack);
                                stack.push(val);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Unable to evaluate expression '" + this + "': " + t.getMessage(), t);
        }
        if (stack.size() > 0) {
            // If there are undefined symbols at this point, it is because
            // there are undefined functions in the expression.
            // (This error should really be caught at an earlier stage.) 
            for (Object o: stack) {
                if (o instanceof Symbol) {
                    Symbol s = (Symbol) o;
                    if (!s.isDefined(ctx)) {
                        throw new RuntimeException("Undefined symbol: " + s.getSymbol());
                    }
                }
            }
        }
        if (stack.size() != 1) {
            throw new RuntimeException("Unable to evaluate expression " + this);
        }
        return stack.pop();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        Iterator<Argument> iter = this.infix.iterator();
        while (iter.hasNext()) {
            Argument arg = iter.next();
            sb.append(arg.getRawValue());
            if (iter.hasNext()) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
    
    private void addFunction(Function function) {
        if (function == null) {
            throw new IllegalArgumentException("Function is NULL");
        }
        Symbol symbol = function.getSymbol();
        if (symbol == null) {
            throw new IllegalArgumentException("Function's symbol is NULL");
        }
        if (this.functions.containsKey(symbol)) {
            throw new IllegalArgumentException("Cannot re-define " + symbol.getSymbol());
        }
        this.functions.put(symbol, function);
    }

    
    private Object defineCollection(EvalStack stack) {
        if (stack.isEmpty()) {
            throw new RuntimeException("Empty evaluation stack");
        }
        Object top = stack.pop();
        if (!(top instanceof Number)) {
            throw new RuntimeException("Number of entries not a numeric value: " + top);   
        }
        int n = ((Number) top).intValue();
        List<Object> list = new ArrayList<Object>();
        boolean allMapEntries = true;
        for (int i = 0; i < n; i++) {
            if (stack.isEmpty()) {
                throw new RuntimeException("Empty evaluation stack");
            }
            Object o = stack.pop();
            if (!(o instanceof MapEntry)) {
                allMapEntries = false;
            }
            list.add(0, o);
        }
        if (!allMapEntries) {
            return list;
        }
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (Object o: list) {
            MapEntry e = (MapEntry) o;
            map.put(e.key, e.value);
        }
        return map;
    }
    
    private static class MapEntry {
        public Object key;
        public Object value;
    }
    
    private static class Mapping extends Operator {

        public Mapping(Symbol symbol, Notation notation, Precedence precedence) {
            super(symbol, notation, precedence);
        }

        @Override
        public Object eval(Context ctx, EvalStack stack) {
            Object value = stack.pop();
            Object key = stack.pop();
            MapEntry entry = new MapEntry();
            entry.key = key;
            entry.value = value;
            return entry;
        }
    }
    
    private static class Funcall {
        private Symbol name;
        private int args;
        public Funcall(Symbol name, int args) {
            this.name = name;
            this.args = args;
        }
        public String toString() {
            return "function#" + name.getSymbol() + "(" + args + ")";
        }
    }
    
}
