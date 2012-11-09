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
package org.vortikal.resourcemanagement.view.tl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vortikal.repository.PropertySet;
import org.vortikal.text.tl.Context;
import org.vortikal.text.tl.Symbol;
import org.vortikal.text.tl.expr.Function;

public class SemesterSearchSortProvider extends Function {

    public SemesterSearchSortProvider(Symbol symbol) {
        super(symbol, 1);
    }

    @Override
    public Object eval(Context ctx, Object... args) {
        Object arg = args[0];
        Iterator<PropertySet> it = (Iterator<PropertySet>) arg;
        List<PropertySet> sorted = new ArrayList<PropertySet>();

        Pattern pattern = Pattern.compile("([hv]{1})(\\d{2})");

        Semester head = null;

        while (it.hasNext()) {
            PropertySet ps = it.next();

            Matcher matcher = pattern.matcher(ps.getURI().getName());

            if (matcher.matches()) {
                if (head == null) {
                    head = new Semester(matcher.group(1), Integer.parseInt(matcher.group(2)), ps);
                } else {
                    head.addSemester(new Semester(matcher.group(1), Integer.parseInt(matcher.group(2)), ps));
                }
            }
        }

        if (head != null) {
            sorted = head.getSorted(sorted);
        }

        return sorted.iterator();
    }

    class Semester {
        public String semester;
        public int year;
        private Semester higher, lower;

        private PropertySet ps;

        public Semester(String semester, int year, PropertySet ps) {
            this.semester = semester;
            this.year = year;
            this.ps = ps;
        }

        public void addSemester(Semester semester) {
            if (semester.year > year) {
                addHigher(semester);
            } else if (semester.year < year) {
                addLower(semester);
            } else {
                if (semester.semester.equals("v")) {
                    addHigher(semester);
                } else {
                    addLower(semester);
                }
            }
        }

        private void addHigher(Semester semester) {
            if (higher != null) {
                higher.addSemester(semester);
            } else {
                higher = semester;
            }
        }

        private void addLower(Semester semester) {
            if (lower != null) {
                lower.addSemester(semester);
            } else {
                lower = semester;
            }
        }

        public List<PropertySet> getSorted(List<PropertySet> sorted) {
            if (higher != null) {
                higher.getSorted(sorted);
            }
            sorted.add(ps);
            if (lower != null) {
                lower.getSorted(sorted);
            }

            return sorted;
        }

    }

}
