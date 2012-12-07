/* Copyright (c) 2012, University of Oslo, Norway
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
package org.vortikal.web.actions.publish;

import java.util.ArrayList;
import java.util.List;

import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyImpl;
import org.vortikal.repository.ResourceImpl;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.store.PrincipalMetadata;
import org.vortikal.repository.store.PrincipalMetadataImpl;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalImpl;

import junit.framework.TestCase;

public class ApprovalViaEmailControllerTest extends TestCase {
    
    private ApprovalViaEmailController approvalViaEmailController;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        approvalViaEmailController = new ApprovalViaEmailController();
        approvalViaEmailController.setEditorialContactsPropDef(null);
        approvalViaEmailController.setPrincipalFactory(new MockPrincipalFactory());
    }

    public void testGetPrincipalLDAPEmail() {
        boolean principalEmailLDAPFound = false;
        assertEquals("oyvind.hatland@usit.uio.no", 
                     approvalViaEmailController.getPrincipalLDAPEmail("oyvihatl@uio.no", principalEmailLDAPFound));
        assertEquals("geir@ntnu.no", 
                     approvalViaEmailController.getPrincipalLDAPEmail("geir@ntnu.no", principalEmailLDAPFound));
    }

    public void testGetEditorialContactEmails() {
        assertEquals("vortex-core@usit.uio.no, resin@ulrik.uio.no",
                     approvalViaEmailController.getEditorialContactEmails(new MockResource(Path.ROOT)));
    }
}

class MockResource extends ResourceImpl {
    public MockResource(Path uri) {
        super(uri);
    }
    public Property getProperty(PropertyTypeDefinition type) {
        if(type == null) {
           return new MockPropertyImpl();
        }
        return null;
    }
}

class MockPropertyImpl extends PropertyImpl {
    @Override
    public Value[] getValues() {
        Value[] vs = new Value[2];
        vs[0] = new Value("vortex-core@usit.uio.no", Type.STRING);
        vs[1] = new Value("resin@ulrik.uio.no", Type.STRING);
        return vs;
    }
}

class MockPrincipalFactory extends PrincipalFactory {
    @Override
    public Principal getPrincipal(String id, Principal.Type type)
            throws InvalidPrincipalException {
        return new MockPrincipalImpl(id, type);
    }
}

@SuppressWarnings("serial")
class MockPrincipalImpl extends PrincipalImpl {
    public MockPrincipalImpl(String id, Type type) throws InvalidPrincipalException {
        super(id, type);
    }
    @Override
    public PrincipalMetadata getMetadata() {
       return new MockPrincipalMetadata(super.getQualifiedName());
    }
}

class MockPrincipalMetadata extends PrincipalMetadataImpl {
    public MockPrincipalMetadata(String qualifiedName) {
        super(qualifiedName);
    }
    @Override
    public List<Object> getValues(String attributeName) {
        if ("email".equals(attributeName)) { // TODO: maybe dig a little deeper into DAO-code etc. to make the test better
            List<Object> emails = new ArrayList<Object>();
            if ("oyvihatl@uio.no".equals(super.getQualifiedName())) {
                emails.add(new String("oyvind.hatland@usit.uio.no"));
            }
            return emails;
        }
        return null;
    }
}