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
package org.vortikal.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ListPartitionerTest {

    @Test
    public void testSplit() {

        List<String> stringList = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
        List<List<String>> partitionedStringList = ListPartitioner.partition(stringList, 2);

        assertTrue(partitionedStringList.size() == 4);
        assertTrue(partitionedStringList.get(0).equals(Arrays.asList("a", "b")));
        assertTrue(partitionedStringList.get(3).equals(Arrays.asList("g")));

        List<Integer> intList = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> partitionedIntList = ListPartitioner.partition(intList, 3);

        assertTrue(partitionedIntList.size() == 2);
        assertTrue(partitionedIntList.get(0).equals(Arrays.asList(1, 2, 3)));
        assertTrue(partitionedIntList.get(1).equals(Arrays.asList(4, 5)));

        List<Boolean> boolList = Arrays.asList(false, true, true, false);
        List<List<Boolean>> partitionedBoolList = ListPartitioner.partition(boolList, 4);

        assertTrue(partitionedBoolList.size() == 1);
        assertTrue(partitionedBoolList.get(0).equals(boolList));

    }

}
