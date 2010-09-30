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

import org.vortikal.repository.resourcetype.Value;
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
        Object o3 = args[2]; // lengthThreshold
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
        
        //XXX: probably use ValNodeFactory for this. ***
        StringBuilder sb = new StringBuilder();
        if(o1 instanceof Value[]) {
          Value[] values = (Value[]) o1;
          for(Value value : values) {
            sb.append(value.getStringValue());
          }
        } else if (o1 instanceof Value) {
          Value value = (Value) o1;
          sb.append(value.getStringValue());
        } else if (o1 instanceof String) {
          sb.append((String) o1);  
        } else {
          throw new IllegalArgumentException("Split: first argument must be Value[], Value or String");  
        }
        //***
        
        String word = sb.toString();
        String splitChar = (String) o2;
        
        int lengthThreshold = -1;
        
        try {
          lengthThreshold = Integer.parseInt((String) o3);
        } catch(NumberFormatException nfex) {
          throw new IllegalArgumentException("Split: third argument must be an integer");
        }
        
        int splitLimit = 0; //split all
        
        try {
          splitLimit = Integer.parseInt((String) o4);
        } catch(NumberFormatException nfex) {
          throw new IllegalArgumentException("Split: fourth argument must be an integer");
        }
        
        if(word.length() > lengthThreshold || lengthThreshold == -1) {
          String[] splittedWords = word.split(splitChar, splitLimit);
          StringBuilder splitted = new StringBuilder();
          
          int splittedWordsLengthMinusOne = splittedWords.length - 1;
          for(int i = 0; i < splittedWordsLengthMinusOne; i++) {
            splitted.append(splittedWords[i] + splitChar + " ");
          }
          splitted.append(splittedWords[splittedWordsLengthMinusOne]);
          
          return splitted.toString();
        }

        return o1; 
        
    }

}