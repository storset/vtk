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
package org.vortikal.repository.index.consistency;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Path;
import org.vortikal.repository.PropertySetImpl;
import org.vortikal.repository.index.IndexException;
import org.vortikal.repository.index.PropertySetIndex;
import org.vortikal.security.Principal;

/**
 * Consistency error where a property exists in the repository, but not in the index.
 * 
 * @author oyviste
 *
 */
public class MissingInconsistency extends RequireOriginalDataConsistencyError {
    
    private static final Log LOG = LogFactory.getLog(MissingInconsistency.class);

    
    public MissingInconsistency(Path uri, PropertySetImpl repositoryPropSet,
                                Set<Principal> aclReadPrincipals) {
        super(uri, repositoryPropSet, aclReadPrincipals);
    }

    @Override
    public boolean canRepair() {
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Property set in repository at URI '" + getUri() + "' does not exist in index.";
    }
    
    @Override
    public String toString() {
        return "MissingInconsistency[URI='" + getUri() + "']";
    }

    /**
     * Fix by adding missing property set.
     */
    @Override
    protected void repair(PropertySetIndex index) throws IndexException {
        LOG.info("Repairing missing inconsistency by adding property set at URI '" 
                + getUri() + "'");
        
        index.addPropertySet(super.repositoryPropSet, super.repositoryAclReadPrincipals);
    }

}
