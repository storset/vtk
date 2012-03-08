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

import org.vortikal.repository.resourcetype.PropertyType.Type;

/**
 * Interface for a <code>Value</code> "factory". It currently only does
 * {@link Value} creation from string representation and binary values.
 */
public interface ValueFactory {

    /**
     * 
     * @param stringValues An array of String representation
     * @param type The type of the Value, see {@link PropertyType}
     * @return An array of Values 
     * @throws ValueFormatException
     */
    public Value[] createValues(String[] stringValues, Type type) throws ValueFormatException;

    /**
     * Create a <code>Value</code> object from the given string
     * representation and type.
     * @param stringValue The String representation of the value
     * @param type The type of the Value, see {@link PropertyType}
     * @return The Value based on the stringValue and type
     * @throws ValueFormatException
     */
    public Value createValue(String stringValue, Type type) throws ValueFormatException;
    
    /**
     * Create Value from binary value.
     * @param value
     * @return
     * @throws ValueFormatException 
     */
    public Value createValue(BinaryValue value) throws ValueFormatException;
    
    /**
     * Create values from binary value.
     * @param value
     * @return
     * @throws ValueFormatException 
     */
    public Value[] createValues(BinaryValue[] value) throws ValueFormatException;
    
}