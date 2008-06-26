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
package org.vortikal.edit.xml;

import junit.framework.TestCase;

import org.jdom.Document;

/** This test case is commented out, because the xml has to reference an external schema url 
 * for the validator to work. If you want to run it, just uncomment the commented code...
 *
 */
public class ValidatorTest extends TestCase {

//    private static final String VALIDATING_XML = "org/vortikal/edit/xml/validating.xml";

    Document validating;

//    protected void setUp() throws Exception {
//        super.setUp();
//
//        SAXBuilder builder = new SAXBuilder(
//                "org.apache.xerces.parsers.SAXParser");
//        
//        builder.setValidation(true);
//
//        /* turn on schema support */
//        builder.setFeature("http://apache.org/xml/features/validation/schema",
//                true);
//       
//        URL validatingXML = this.getClass().getClassLoader()
//        .getResource(VALIDATING_XML);
//
//        builder.setXMLFilter(new XMLSpaceCorrectingXMLFilter());
//        validating = builder.build(validatingXML);
//
//    }

    public void testValidate() {
//        try {
//            new Validator().validate(validating);
//    
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail("Shouldn't fail validation: " + e.getMessage());
//        }
//
//        Element element = new Element("fritekst");
//        element.addContent("Should fail");
//        validating.getRootElement().addContent(element);
//
//        try {
//            new Validator().validate(validating);
//            fail("Shouldn't validate");
//        } catch (XMLEditException e) {
//            if (!(e.getCause() instanceof JDOMException)) {
//                fail("Cause should be JDOMException, not " + e.getCause().getClass().getName());
//            }
//            // Expected
//        } catch (Exception e) {
//            fail("Should fail with XMLEditException, not: " 
//                    + e.getClass().getName() + " - "+ e.getMessage());
//        }
    }
}