/* Copyright (c) 2014, University of Oslo, Norway
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
package vtk.util.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import vtk.util.io.StreamUtil;

/**
 * Test {@link vtk.util.text.Json}.
 */
public class JsonTest {
 
    // Mock JSON data
    public static final String JSON_TEST_RESOURCE = "JsonTest.txt";
    
    private String jsonTestText;
    private InputStream jsonInputStream;
    
    @Before
    public void setUp() throws IOException {
        InputStream is = getClass().getResourceAsStream(JSON_TEST_RESOURCE);
        jsonTestText = StreamUtil.streamToString(is, "utf-8");
        jsonInputStream = getClass().getResourceAsStream(JSON_TEST_RESOURCE);
    }
    
    @Test
    public void testParseFromString() {
        List<Object> result = (List<Object>) Json.parse(jsonTestText);
        assertEquals(3500, result.size());
    }

    @Test
    public void testParseFromStream() throws IOException {
        List<Object> result = (List<Object>) Json.parse(jsonInputStream);
        assertEquals(3500, result.size());
    }

    @Test
    public void testParseFromReader() throws IOException {
        List<Object> result = (List<Object>) Json.parse(new InputStreamReader(jsonInputStream));
        assertEquals(3500, result.size());
    }
    
    @Test
    public void testParseData() {
        List<Object> result = (List<Object>) Json.parse(jsonTestText);
        assertEquals(3500, result.size());
        
        Map<String,Object> first = (Map<String,Object>)result.get(0);
        assertEquals(Long.valueOf(1), first.get("id"));
        assertEquals("Brazil", first.get("country"));
        assertEquals(10, ((List<Object>)first.get("numbers")).size());
        
        Map<String,Object> last = (Map<String,Object>)result.get(3499);
        assertEquals(Long.valueOf(4000), last.get("id"));
        assertEquals("Indonesia", last.get("country"));
        assertEquals(10, ((List<Object>)last.get("numbers")).size());
    }
    
    @Test
    public void testSelect() {
        Json.Container c = Json.parseToContainer(jsonTestText);
        Json.MapContainer o = c.asArray().objectValue(120);
        
        assertEquals((Long)121l, o.select("id"));
        assertEquals("Judith", o.select("name.first_name"));
        assertEquals("Alexander", o.select("name.last_name"));
        assertEquals("jalexander3c", o.select("name.username"));
        assertEquals("Russia", o.select("country"));
        assertEquals("#95472d", o.select("colors[0]"));
        assertEquals(25.08, o.select("numbers[1]"));
    }
    
    @Test
    public void testParseToContainer() {
        Json.Container c = Json.parseToContainer(jsonTestText);
        assertEquals(3500, c.size());
        
        assertTrue(c.isArray());
        Json.MapContainer first = c.asArray().objectValue(0);
        assertEquals((Long)1l, first.longValue("id"));
        assertEquals("Brazil", first.stringValue("country"));
        assertEquals(10, first.arrayValue("numbers").size());

        Json.MapContainer last = c.asArray().objectValue(3499);
        assertEquals((Long)4000l, last.longValue("id"));
        assertEquals("Indonesia", last.stringValue("country"));
        assertEquals(10, last.arrayValue("numbers").size());
        }
    
    // Compare performance between vtk.util.text.JSON and vtk.util.text.Json
    @Ignore
    @Test
    public void testPerformance() {
        int count=10;
        Object r1=null, r2=null;
        for (int i=0; i<count; i++) {
            r1 = vtk.util.text.JSON.parse(jsonTestText);
            r2 = Json.parse(jsonTestText);
        }
        System.out.println("Warmup complete.");
        
        List<Object> l1 = (List<Object>)r1;
        List<Object> l2 = (List<Object>)r2;
        System.out.println("Elements in l1: " + l1.size());
        System.out.println("Elements in l2: " + l2.size());

        assertEquals(l1.size(), l2.size());
        
        long start = System.currentTimeMillis();
        for (int i=0; i<count; i++) {
            vtk.util.text.JSON.parse(jsonTestText);
        }
        long end = System.currentTimeMillis();
        System.out.println("vtk.util.text.JSON.parse(String) took " + (end-start) + "ms");
        
        start = System.currentTimeMillis();
        for (int i=0; i<count; i++) {
            Json.parse(jsonTestText);
        }
        end = System.currentTimeMillis();
        System.out.println("vtk.util.text.Json.parse(String) took " + (end-start) + "ms");
    }
}
