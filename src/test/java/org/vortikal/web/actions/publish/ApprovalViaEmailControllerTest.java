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

import java.util.Arrays;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;

import org.vortikal.repository.Property;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.repository.resourcetype.Value;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.store.PrincipalMetadataImpl;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.security.PrincipalImpl;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.vortikal.repository.Resource;
import org.vortikal.repository.store.Metadata;
import org.vortikal.repository.store.PrincipalMetadata;
import org.vortikal.repository.store.PrincipalMetadataDAO;

public class ApprovalViaEmailControllerTest {

    @Rule
    public final JUnitRuleMockery context = new JUnitRuleMockery();
    
    private ApprovalViaEmailController approvalViaEmailController;
    private PrincipalMetadataDAO pmDao;
    private PropertyTypeDefinition editorialContactsPropDef;

    @Before
    public void setUp() throws Exception {
        pmDao = context.mock(PrincipalMetadataDAO.class);
        editorialContactsPropDef = context.mock(PropertyTypeDefinition.class);
        
        approvalViaEmailController = new ApprovalViaEmailController();
        approvalViaEmailController.setEditorialContactsPropDef(editorialContactsPropDef);
        PrincipalFactory pf = new PrincipalFactory();
        pf.setPrincipalMetadataDao(pmDao);
        approvalViaEmailController.setPrincipalFactory(pf);
    }

    @Test
    public void getUserEmail() {
        final PrincipalImpl principal = new PrincipalImpl("oyvihatl@uio.no", Principal.Type.USER);

        final PrincipalMetadata pm = context.mock(PrincipalMetadata.class);
        
        context.checking(new Expectations() {
            {
                oneOf(pmDao).getMetadata(principal, null);
                will(returnValue(pm));
                
                oneOf(pm).getValue(PrincipalMetadataImpl.DESCRIPTION_ATTRIBUTE); will(returnValue(null));
                oneOf(pm).getValue(Metadata.URL_ATTRIBUTE); will(returnValue(null));

                oneOf(pm).getValues("email");
                will(returnValue(Arrays.asList(new String[] {"oyvind.hatland@usit.uio.no"})));
            }
        });
        
        assertEquals("oyvind.hatland@usit.uio.no", approvalViaEmailController.getUserEmail("oyvihatl@uio.no"));
        assertNull(approvalViaEmailController.getUserEmail("geir@ntnu.no"));
    }

    @Test
    public void getEditorialContactEmails() {
        
        final Resource resource = context.mock(Resource.class);
        final Property property = context.mock(Property.class);
        final Value[] values = new Value[] {
            new Value("vortex-core@usit.uio.no", Type.STRING),
            new Value("resin@ulrik.uio.no", Type.STRING)
        };
        
        context.checking(new Expectations(){
            {
                oneOf(resource).getProperty(editorialContactsPropDef);
                will(returnValue(property));
                oneOf(property).getValues();
                will(returnValue(values));
            } 
        });
        
        assertEquals("vortex-core@usit.uio.no, resin@ulrik.uio.no",
                approvalViaEmailController.getEditorialContactEmails(resource));
    }
}
