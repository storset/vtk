/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal.web.actions.properties;

import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;


/**
 * Interface for allowing hooks to be run when creating, removing or
 * modifying resource properties using {@link PropertyEditController}.
 */
public interface PropertyEditHook {


    /**
     * Method invoked when a property is created.
     *
     * @param def the created property's definition
     * @param resource the resource on which the property is created
     * @exception Exception if an error occurs
     */
    public void created(PropertyTypeDefinition def, Resource resource) throws Exception;
    

    /**
     * Method invoked when a property is removed.
     *
     * @param def removed property's definition
     * @param resource the resource on which the property is removed
     * @exception Exception if an error occurs
     */
    public void removed(PropertyTypeDefinition def, Resource resource) throws Exception;


    /**
     * Method invoked when a property is modified. (Also called on
     * property creation).
     *
     * @param def the modified property's definition
     * @param resource the resource on which the property is modified
     * @exception Exception if an error occurs
     */
    public void modified(PropertyTypeDefinition def, Resource resource) throws Exception;

}
