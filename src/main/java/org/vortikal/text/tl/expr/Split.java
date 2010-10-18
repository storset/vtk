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

import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;

public class Split extends Function {

    public Split(Symbol symbol) {
        super(symbol, 4);
    }

    @Override
    public Object eval(Context ctx, Object... args) {
        Object o1 = args[0]; // word
        Object o2 = args[1]; // splitChar
        Object o3 = args[2]; // threshold
        Object o4 = args[3]; // splitLimit
        
        if (o1 == null) {
            throw new IllegalArgumentException("Split: first argument is NULL");
        }
        if (o2 == null) {
            throw new IllegalArgumentException("Split: second argument is NULL");
        }
        if (o3 == null) {
            throw new IllegalArgumentException("Split: third argument is NULL");
        }
        if (o4 == null) {
            throw new IllegalArgumentException("Split: fourth argument is NULL");
        }
        
        String word = o1.toString();
        String splitChar = (String) o2;
        
        int threshold = -1;
        
        try {
          threshold = ((Integer)o3).intValue();
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Split: third argument must be an integer");
        }
        int splitLimit = 0;
        try {
          splitLimit = ((Integer)o4).intValue();
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Split: fourth argument must be an integer");
        }
        
        if (word.length() > threshold || threshold == -1) {
          String[] splitWords = word.split(splitChar, splitLimit);
          StringBuilder result = new StringBuilder();
          for (int i = 0; i < splitWords.length - 1; i++) {
            result.append(splitWords[i] + splitChar + " ");
          }
          result.append(splitWords[splitWords.length - 1]);
          return result.toString();
        }
        return o1; 
    }

}