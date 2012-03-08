/* Copyright (c) 2010, University of Oslo, Norway
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Literal;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.Token;

public class Accessor extends Operator {
    private Token field;

    public Accessor(Symbol symbol, Token field) {
        super(symbol);
        this.field = field;
    }

    @Override
    public Object eval(Context ctx, ExpressionNode... nodes) {
        Object collection = nodes[0].eval(ctx);
        Object accessor = (this.field instanceof Literal) ? 
                this.field.getValue(ctx) : this.field;
        if (collection == null) {
            throw new IllegalArgumentException("First argument is NULL");
        }
        if (accessor == null) {
            throw new IllegalArgumentException("Second argument is NULL");
        }
        if (collection.getClass().isArray()) {
            return accessArray((Object[]) collection, accessor);
            
        } else if (collection instanceof Collection<?>) {
            return accessList((Collection<?>) collection, accessor);
            
        } else if (collection instanceof Map<?, ?>) {  
            return accessMap((Map<?, ?>) collection, accessor);
        } 
        throw new IllegalArgumentException("Unable to access field '" 
                + accessor + "' of object '" + collection + "'");
    }

    private Object accessArray(Object[] array, Object accessor) {
        int i = getNumericValue(accessor).intValue();
        int n = array.length;
        if (i < 0 || (n == 0 && i == 0) || i > n) {
            throw new IllegalArgumentException("Index out of bounds: " + i);
        }
        return array[i];
    }
    
    private Object accessList(Collection<?> collection, Object accessor) {
        List<?> list = (List<?>) collection;
        int i = getNumericValue(accessor).intValue();
        int n = list.size();
        if (i < 0 || (n == 0 && i == 0) || i > n) {
            throw new IllegalArgumentException("Index out of bounds: " + i);
        }
        return list.get(i);
    }
    
    private Object accessMap(Map<?, ?> map, Object accessor) {
        if (!(accessor instanceof Symbol)) {
            throw new IllegalArgumentException("Accessor '" + accessor + "' is not a symbol");
        }
        accessor = ((Symbol) accessor).getSymbol();
        return map.get(accessor);
    }
    
}