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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Utility class for parsing and evaluating infix expressions 
 * (e.g. <code>(x = y) || (x = z)</code>).
 */
public class Expression {

    /**
     * Operator interface
     */
    public interface Operator {
        public Symbol getSymbol();
        public Presedence getPresedence();
        public Object eval(Stack<Object> stack);
    }

    public static final Symbol EQ = new Symbol("=");
    public static final Symbol NEQ = new Symbol("!=");
    public static final Symbol AND = new Symbol("&&");
    public static final Symbol OR = new Symbol("||");
    public static final Symbol NOT = new Symbol("!");
    public static final Symbol PLUS = new Symbol("+");
    public static final Symbol MINUS = new Symbol("-");
    public static final Symbol MULTIPLY = new Symbol("*");
    public static final Symbol DIVIDE = new Symbol("/");

    /**
     * The default set of operators
     */
    public static final Map<Symbol, Operator> DEFAULT_OPERATORS;
    static {
        Map<Symbol, Operator> ops = new HashMap<Symbol, Operator>();

        // Unary
        ops.put(NOT, new Not(NOT, Presedence.EIGHT));

        // Multiplicative
        ops.put(DIVIDE, new Divide(DIVIDE, Presedence.SEVEN));
        ops.put(MULTIPLY, new Multiply(MULTIPLY, Presedence.SIX));

        // Additive
        ops.put(PLUS, new Plus(PLUS, Presedence.FIVE));
        ops.put(MINUS, new Minus(MINUS, Presedence.FOUR));

        // Equality
        ops.put(EQ, new Eq(EQ, Presedence.THREE));
        ops.put(NEQ, new Neq(NEQ, Presedence.TWO));

        // Logical AND/OR
        ops.put(AND, new And(AND, Presedence.ONE));
        ops.put(OR, new Or(OR, Presedence.ZERO));

        DEFAULT_OPERATORS = Collections.unmodifiableMap(new HashMap<Symbol, Operator>(ops));
    }

    /**
     * The set of defined operator presedences
     */
    public static enum Presedence {
        ZERO(0),
        ONE(1),
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8);
        private int n;
        private Presedence(int n) {
            this.n = n;
        }
        public int value() {
            return this.n;
        }
    }

    /**
     * The set of defined operators for this expression
     */
    private Map<Symbol, Operator> operators;

    /**
     * The expression in its original infix notation
     */
    private List<Argument> infix;

    /**
     * The expression converted to postfix notation
     */
    private List<Argument> postfix;

    /**
     * Constructs an expression using the default set of operators
     */
    public Expression(List<Argument> args) {
        this(DEFAULT_OPERATORS, args);
    }

    /**
     * Constructs an expression using a supplied set of operators
     */
    public Expression(Map<Symbol, Operator> operators,
            List<Argument> args) {
        if (operators == null || operators.isEmpty()) {
            throw new IllegalArgumentException("Empty operator set");
        }
        this.operators = new HashMap<Symbol, Operator>(operators);

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        this.infix = new ArrayList<Argument>(args);

        Stack<Symbol> stack = new Stack<Symbol>();
        List<Argument> expression = new ArrayList<Argument>();

        for (Argument arg : args) {
            if (arg instanceof Literal) {
                expression.add(arg);
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
                    expression.add(top);
                }
                continue;
            }

            Operator op = this.operators.get(symbol);
            if (op == null) {
                expression.add(symbol);
                continue;
            }

            if (stack.isEmpty()) {
                stack.push(symbol);
                continue;
            }
            Operator top = this.operators.get(stack.peek());
            int n = op.getPresedence().value();
            while (top != null && top.getPresedence().value() >= n) {
                expression.add(top.getSymbol());
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
            expression.add(stack.pop());
        }
        this.postfix = expression;
    }

    /**
     * Evaluates the expression. Returns a single 
     * object value as the result.
     */
    public Object evaluate(Context ctx) {
        Stack<Object> stack = new Stack<Object>();
        for (Argument arg : this.postfix) {
            if (arg instanceof Literal) {
                stack.push(arg.getValue(ctx));
            } else {
                Symbol s = (Symbol) arg;
                Operator op = operators.get(s);
                if (op == null) {
                    // XXX: in case a symbol is undefined, 
                    // we push NULL onto the stack. This means 
                    // that undefined symbols in expressions 
                    // will not necessarily be treated as errors:
                    if (s.isDefined(ctx)) {
                        Object val = s.getValue(ctx);
                        stack.push(val);
                    } else {
                        stack.push(null);
                    }
                } else {
                    try {
                        Object val = op.eval(stack);
                        stack.push(val);
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException("Unable to evaluate expression '" 
                                + this.toString() + "': " + t.getMessage(), t);
                    }
                }
            }
        }
        if (stack.size() != 1) {
            throw new RuntimeException("Unable to evaluate expression " + this.toString());
        }
        return stack.peek();
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

    public static abstract class AbstractOperator implements Operator {
        private Symbol symbol;
        private Presedence presedence;

        public AbstractOperator(Symbol symbol, Presedence presedence) {
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

    public static class Eq extends AbstractOperator {
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

    public static class Neq extends AbstractOperator {
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

    public static class And extends AbstractOperator {
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

    public static class Or extends AbstractOperator {
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

    public static class Not extends AbstractOperator {
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

    public static abstract class NumericOperator extends AbstractOperator {

        public NumericOperator(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }

        public final Object eval(Stack<Object> stack) {
            Object second = stack.pop();
            Object first = stack.pop();
            Integer i1 = getIntegerValue(first);
            Integer i2 = getIntegerValue(second);
            return evalNumeric(i1, i2);
        }

        protected abstract Object evalNumeric(Integer i1, Integer i2);

        private Integer getIntegerValue(Object obj) {
            if (obj == null) {
                throw new IllegalArgumentException("Argument is NULL");
            }

            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            } else if (obj instanceof String) {
                try {
                    String s = (String) obj;
                    return Integer.valueOf(Integer.parseInt(s));
                } catch (NumberFormatException e) { 
                }
            }
            throw new IllegalArgumentException("Not a number: " + obj);
        }
    }

    public static class Plus extends NumericOperator {
        public Plus(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }

        protected Object evalNumeric(Integer i1, Integer i2) {
            return Integer.valueOf(i1 + i2);
        }
    }

    public static class Minus extends NumericOperator {

        public Minus(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }

        protected Object evalNumeric(Integer i1, Integer i2) {
            return Integer.valueOf(i1 - i2);
        }
    }

    public static class Multiply extends NumericOperator {

        public Multiply(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }

        protected Object evalNumeric(Integer i1, Integer i2) {
            return Integer.valueOf(i1 * i2);
        }
    }

    public static class Divide extends NumericOperator {

        public Divide(Symbol symbol, Presedence presedence) {
            super(symbol, presedence);
        }

        protected Object evalNumeric(Integer i1, Integer i2) {
            return Integer.valueOf(i1 / i2);
        }
    }
}
