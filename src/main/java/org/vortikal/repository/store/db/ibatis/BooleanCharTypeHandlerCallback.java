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
package org.vortikal.repository.store.db.ibatis;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;
import java.sql.SQLException;


public class BooleanCharTypeHandlerCallback implements TypeHandlerCallback {

    private static final String TRUE = "Y"; 
    private static final String FALSE = "N"; 

    @Override
    public Object getResult(ResultGetter getter) throws SQLException { 
        String value = getter.getString();
        if (TRUE.equals(value)) {
            return Boolean.TRUE;
        } else if (FALSE.equals(value)) {
            return Boolean.FALSE;
        } else {
            throw new SQLException(
                "Unable to handle value '" + value
                + "': expected one of '" + TRUE + "', '" + FALSE + "'");
        }
    } 

    @Override
    public void setParameter(ParameterSetter setter, Object parameter) throws SQLException { 
        Boolean value = ((Boolean) parameter).booleanValue();
        if (value) {
            setter.setString(TRUE);
        } else {
            setter.setString(FALSE);
        }
    } 

    @Override
    public Object valueOf(String s) { 
        if (TRUE.equals(s)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    } 
}
    
