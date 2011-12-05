/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.resourcemanagement.edit;

import java.util.List;

import org.vortikal.repository.Path;
import org.vortikal.resourcemanagement.StructuredResource;
import org.vortikal.resourcemanagement.StructuredResourceDescription;
import org.vortikal.resourcemanagement.StructuredResourceTestSetup;
import org.vortikal.web.service.URL;

public class FormSubmitCommandTestIntegration extends StructuredResourceTestSetup {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCreateForm() {
        StructuredResourceDescription srd = srdp.getResourceDescription("person");
        URL url = new URL("http", "localhost:9321", Path.ROOT);
        FormSubmitCommand fsc = new FormSubmitCommand(new StructuredResource(srd), url,url, false, true);
        printForm(fsc);
    }

    private void printForm(FormSubmitCommand fsc) {
        System.out.println(fsc.getResource().getType().getName());
        List<FormElementBox> elements = fsc.getElements();
        if (elements != null && elements.size() > 0) {
            System.out.println("  formelements: ");
            printBoxElements(elements);
        }
    }

    private void printBoxElements(List<FormElementBox> elements) {
        for (FormElementBox elementBox : elements) {
            System.out.println("    " + elementBox.getName());
            List<FormElement> formElements = elementBox.getFormElements();
            if (formElements.size() > 1) {
                printGroupedFormElements(formElements);
            }
        }
    }

    private void printGroupedFormElements(List<FormElement> formElements) {
        for (FormElement formElement : formElements) {
            System.out.println("      " + formElement.getDescription().getName());
        }
    }

}
