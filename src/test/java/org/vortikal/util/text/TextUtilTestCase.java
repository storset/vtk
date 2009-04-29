package org.vortikal.util.text;

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
import junit.framework.TestCase;

public class TextUtilTestCase extends TestCase {

    public void testRemoveDuplicatesIgnoreCase() {

        // OLD: String testData = "Forskning, Røed Ødegård, forskning, FoRsKning, forskNING";
        String testData = "oFFentlig helseVesen, jon blund, Offentlig helsevesen, Jon Blund";

        String expectedData = "oFFentlig helseVesen, jon blund";
        String expectedDataNoSpaces = "oFFentlig helseVesen,jon blund";
        String expectedDataNoDelimiter = "oFFentlig helseVesen jon blund";
        String expectedDataNoSpacesAndDelimiter = "oFFentlig helseVesenjon blund";
        String expectedDataCapitalized = "OFFentlig HelseVesen, Jon Blund";

        // Test noSpaces, noDelimiter boolean configurations
        String test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", false, false, false);
        assertEquals(expectedData, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", true, false, false);
        assertEquals(expectedDataNoSpaces, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", false, true, false);
        assertEquals(expectedDataNoDelimiter, test);

        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",", true, true, false);
        assertEquals(expectedDataNoSpacesAndDelimiter, test);

        // Overload Methods
        test = TextUtils.removeDuplicatesIgnoreCase(testData, ",");
        assertEquals(expectedData, test);

        test = TextUtils.removeDuplicatesIgnoreCaseCapitalized(testData, ",");
        assertEquals(expectedDataCapitalized, test);

    }
}
