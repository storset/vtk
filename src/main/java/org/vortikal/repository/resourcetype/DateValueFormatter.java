/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.FastDateFormat;
import org.vortikal.util.repository.LocaleHelper;

public class DateValueFormatter implements ValueFormatter {

    private String defaultDateFormatKey = "long";
    private Locale defaultLocale = new Locale("en");

    private Map<String, FastDateFormat> namedDateFormats = new HashMap<String, FastDateFormat>();
    private Set<String> recognizedLocales = new HashSet<String>();
    private boolean date = false;

    public DateValueFormatter() {
        this.recognizedLocales.add("no");
        this.recognizedLocales.add("nn");
        this.recognizedLocales.add("en");

        this.namedDateFormats.put("short_en", FastDateFormat.getInstance("MMM d, yyyy", new Locale("en")));
        this.namedDateFormats.put("short_no", FastDateFormat.getInstance("d. MMM. yyyy", new Locale("no")));
        this.namedDateFormats.put("short_nn", FastDateFormat.getInstance("d. MMM. yyyy", new Locale("no")));
        this.namedDateFormats.put("long_en", FastDateFormat.getInstance("MMM d, yyyy hh:mm a", new Locale("en")));
        this.namedDateFormats.put("long_no", FastDateFormat.getInstance("d. MMM. yyyy HH:mm", new Locale("no")));
        this.namedDateFormats.put("long_nn", FastDateFormat.getInstance("d. MMM. yyyy HH:mm", new Locale("no")));
        this.namedDateFormats
                .put("longlong_en", FastDateFormat.getInstance("MMM d, yyyy hh:mm:ss a", new Locale("en")));
        this.namedDateFormats.put("longlong_no", FastDateFormat.getInstance("d. MMM. yyyy HH:mm:ss", new Locale("no")));
        this.namedDateFormats.put("longlong_nn", FastDateFormat.getInstance("d. MMM. yyyy HH:mm:ss", new Locale("no")));
        this.namedDateFormats.put("hours-minutes_en", FastDateFormat.getInstance("hh:mm a", new Locale("en")));
        this.namedDateFormats.put("hours-minutes_no", FastDateFormat.getInstance("HH:mm", new Locale("no")));
        this.namedDateFormats.put("hours-minutes_nn", FastDateFormat.getInstance("HH:mm", new Locale("no")));
        this.namedDateFormats.put("iso-8601", FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ssZZ", new Locale("en")));
        this.namedDateFormats.put("iso-8601-short", FastDateFormat.getInstance("yyyy-MM-dd", new Locale("en")));
        this.namedDateFormats.put("rfc-822",
                FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss Z", new Locale("en")));

        this.namedDateFormats.put("full-month-year-short_no",
                FastDateFormat.getInstance("d. MMMM yyyy", new Locale("no")));
        this.namedDateFormats.put("full-month-year-short_nn",
                FastDateFormat.getInstance("d. MMMM yyyy", new Locale("no")));
        this.namedDateFormats.put("full-month-year-short_en",
                FastDateFormat.getInstance("MMMM d, yyyy", new Locale("en")));
        this.namedDateFormats.put("full-month-year_no", FastDateFormat.getInstance("MMMM yyyy", new Locale("no")));
        this.namedDateFormats.put("full-month-year_nn", FastDateFormat.getInstance("MMMM yyyy", new Locale("nn")));
        this.namedDateFormats.put("full-month-year_en", FastDateFormat.getInstance("MMMM yyyy", new Locale("en")));
        this.namedDateFormats.put("full-month-short_no", FastDateFormat.getInstance("d. MMMM", new Locale("no")));
        this.namedDateFormats.put("full-month-short_nn", FastDateFormat.getInstance("d. MMMM", new Locale("no")));
        this.namedDateFormats.put("full-month-short_en", FastDateFormat.getInstance("MMMM d", new Locale("en")));
    }

    public DateValueFormatter(boolean date) {
        this();
        this.date = date;
    }

    public String valueToString(Value value, String format, Locale locale) throws IllegalValueTypeException {

        if (value.getType() != PropertyType.Type.TIMESTAMP && value.getType() != PropertyType.Type.DATE) {
            throw new IllegalValueTypeException(PropertyType.Type.TIMESTAMP, value.getType());
        }

        if (format == null) {
            format = this.defaultDateFormatKey;
        }
        Date date = value.getDateValue();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if (this.date && value.getType() == PropertyType.Type.DATE && format.contains("long")
                && cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0) {
            format = format.replace("long", "short");
        }

        FastDateFormat f = null;

        locale = LocaleHelper.getMessageLocalizationLocale(locale);
        if (locale == null || !recognizedLocales.contains(locale.getLanguage())) {
            locale = this.defaultLocale;
        }
        // Check if format refers to any of the
        // predefined (named) formats:
        String key = format + "_" + locale.getLanguage();

        f = this.namedDateFormats.get(key);
        if (f == null) {
            key = format;
            f = this.namedDateFormats.get(key);
        }

        try {
            if (f == null) {
                // Parse the given format
                // XXX: formatter instances should be cached
                f = FastDateFormat.getInstance(format, locale);
            }
            return f.format(date);
        } catch (Throwable t) {
            return "Error: " + t.getMessage();
        }

    }

    private static final String[] FALLBACK_DATE_FORMATS = new String[] { "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };

    public Value stringToValue(String string, String format, Locale locale) throws IllegalArgumentException {
        if (format == null) {
            format = this.defaultDateFormatKey;
        }
        if (locale == null || !recognizedLocales.contains(locale.getLanguage())) {
            locale = this.defaultLocale;
        }

        FastDateFormat fdf = this.namedDateFormats.get(format + "_" + locale.getLanguage());

        if (fdf == null) {
            fdf = this.namedDateFormats.get(format);
        }

        if (fdf != null) {
            format = fdf.getPattern();
            locale = fdf.getLocale();
        }

        SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
        Date date;
        try {
            date = sdf.parse(string);
        } catch (ParseException e) {
            for (String fallbackFormat : FALLBACK_DATE_FORMATS) {
                try {
                    SimpleDateFormat fsdf = new SimpleDateFormat(fallbackFormat, Locale.getDefault());
                    date = fsdf.parse(string);
                    return new Value(date, this.date);
                } catch (ParseException t) {
                }
            }
            throw new IllegalArgumentException("Unable to parse to date value from '" + string
                    + "' object using string format '" + format + "'", e);
        }
        return new Value(date, this.date);
    }

}
