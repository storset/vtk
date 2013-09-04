/* Copyright (c) 2013, University of Oslo, Norway
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
package org.vortikal.util.repository;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class LocaleHelperTest {

    @Test
    public void testGetLocale() {

        assertExpectedLocaleFromProvidedLangString("unknown", null);
        assertExpectedLocaleFromProvidedLangString("", null);
        assertExpectedLocaleFromProvidedLangString("  ", null);
        assertExpectedLocaleFromProvidedLangString(null, null);

        assertExpectedLocaleFromProvidedLangString("en", new Locale("en"));
        assertExpectedLocaleFromProvidedLangString("en_GB", new Locale("en", "GB"));
        assertExpectedLocaleFromProvidedLangString("en_US", new Locale("en", "US"));

        assertExpectedLocaleFromProvidedLangString("no", new Locale("no"));
        assertExpectedLocaleFromProvidedLangString("no_NO", new Locale("no", "NO"));

        assertExpectedLocaleFromProvidedLangString("no_NO_NY", new Locale("no", "NO", "NY"));
        assertExpectedLocaleFromProvidedLangString("nn", new Locale("nn"));

    }

    private void assertExpectedLocaleFromProvidedLangString(String providedLangString, Locale expectedLocale) {
        Locale actualLocale = LocaleHelper.getLocale(providedLangString);
        assertEquals(expectedLocale, actualLocale);
    }

}
