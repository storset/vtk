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
package org.vortikal.urchin;

import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;

//import org.vortikal.web.RequestContext;

public class UrchinResourceStats implements InitializingBean {
    private String base = "https://statistikk.uio.no/services/v2/reportservice/data";
    private String urchinUser;
    private String urchinPassword;
    private String dimensions = "&dimensions=u:request_stem";
    private String metrics = "&metrics=u:visits";
    private String table = "&table=12";
    private String idAndFilter;

    private CacheManager cacheManager;
    private Cache cache;
    private net.sf.ehcache.Element cached;
    private UrchinRes ur;

    public static class UrchinRes implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String date;
        public int res;
    }

    public int thisMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    public int nMonths() {
        Calendar cal = Calendar.getInstance();

        if (cal.get(Calendar.YEAR) == 2011) {
            if (cal.get(Calendar.MONTH) == 10)
                return 3;
            if (cal.get(Calendar.MONTH) == 11)
                return 4;
        } else if (cal.get(Calendar.YEAR) == 2012) {
            if (cal.get(Calendar.MONTH) == 0)
                return 5;
            if (cal.get(Calendar.MONTH) == 1)
                return 6;
            if (cal.get(Calendar.MONTH) == 2)
                return 7;
            if (cal.get(Calendar.MONTH) == 3)
                return 8;
            if (cal.get(Calendar.MONTH) == 4)
                return 9;
            if (cal.get(Calendar.MONTH) == 5)
                return 10;
        }

        return 11;
    }

    public int[] months(Resource r, String token, String id) {
        Calendar urchinStartCal = Calendar.getInstance();
        urchinStartCal.set(2011, 7, 1);

        int[] months = new int[13];
        boolean now = true;

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 12; i++) {
            if (cal.get(Calendar.MONTH) < urchinStartCal.get(Calendar.MONTH))
                break;

            String sdate = "&start-date=" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
                    + cal.getActualMinimum(Calendar.DATE);

            String edate = "&end-date=" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-";

            if (now) {
                edate += cal.get(Calendar.DATE);
                now = false;
            } else
                edate += cal.getActualMaximum(Calendar.DATE);

            months[cal.get(Calendar.MONTH)] = fetch(r, token, sdate + edate, id, "" + cal.get(Calendar.MONTH));
            if (months[cal.get(Calendar.MONTH)] > months[12]) {
                months[12] = months[cal.get(Calendar.MONTH)];
            }

            if (cal.get(Calendar.MONTH) == 0) {
                cal.set(Calendar.MONTH, 11);
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
            } else
                cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        }

        return months;
    }

    public int total(Resource r, String token, String id) {
        Calendar urchinStartCal = Calendar.getInstance();
        urchinStartCal.set(2011, 7, 1);
        String urchinStartDate = "&start-date=" + urchinStartCal.get(Calendar.YEAR) + "-"
                + (urchinStartCal.get(Calendar.MONTH) + 1) + "-" + urchinStartCal.get(Calendar.DATE);

        Calendar today = Calendar.getInstance();
        String todayDate = "&end-date=" + today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-"
                + today.get(Calendar.DATE);

        return fetch(r, token, urchinStartDate + todayDate, id, "total");
    }

    public int thirtyTotal(Resource r, String token, String id) {
        Calendar ecal = Calendar.getInstance();
        Calendar scal = Calendar.getInstance();

        // Need to do this twice because 86400000 * 30 = -1702967296.
        long half = 86400000 * 15;
        scal.setTimeInMillis(ecal.getTimeInMillis() - half);
        scal.setTimeInMillis(scal.getTimeInMillis() - half);

        String date = "&start-date=" + scal.get(Calendar.YEAR) + "-" + (scal.get(Calendar.MONTH) + 1) + "-"
                + scal.get(Calendar.DATE) + "&end-date=" + ecal.get(Calendar.YEAR) + "-"
                + (ecal.get(Calendar.MONTH) + 1) + "-" + ecal.get(Calendar.DATE);

        return fetch(r, token, date, id, "thirty");
    }

    public int weekTotal(Resource r, String token, String id) {
        Calendar ecal = Calendar.getInstance();
        Calendar scal = Calendar.getInstance();

        scal.setTimeInMillis(ecal.getTimeInMillis() - (86400000 * 7));

        String date = "&start-date=" + scal.get(Calendar.YEAR) + "-" + (scal.get(Calendar.MONTH) + 1) + "-"
                + scal.get(Calendar.DATE) + "&end-date=" + ecal.get(Calendar.YEAR) + "-"
                + (ecal.get(Calendar.MONTH) + 1) + "-" + ecal.get(Calendar.DATE);

        return fetch(r, token, date, id, "week");
    }

    public int yesterdayTotal(Resource r, String token, String id) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cal.getTimeInMillis() - 86400000);

        String date = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DATE);

        return fetch(r, token, "&start-date=" + date + "&end-date=" + date, id, "yesterday");
    }

    private int fetch(Resource r, String token, String date, String id, String key) {
        int sum = 0;

        if (urchinUser.equals("") || urchinPassword.equals(""))
            return sum;

        StringBuilder url = new StringBuilder(base);
        url.append("?login=" + urchinUser + "&password=" + urchinPassword);
        url.append(date);
        url.append(dimensions);
        url.append(metrics);
        url.append(table);
        if ((idAndFilter = setIdAndFilter(id)) != null)
            url.append(idAndFilter);
        else
            return sum;

        if (r.isCollection()) {
            try {
                String expanded = r.getURI().expand("index.html").toString();
                String html = url.toString().concat(expanded);

                if (cache != null)
                    cached = this.cache.get(idAndFilter + expanded + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.date != null && ur.date.equals(date)) {
                    sum += ur.res;
                } else {
                    ur.res = parseDOMToStats(parseXMLFileToDOM(html));
                    ur.date = date;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(idAndFilter + expanded + key, ur));
                }
            } catch (Exception e) {
            }

            try {
                String expanded = r.getURI().expand("index.xml").toString();
                String xml = url.toString().concat(expanded);

                if (cache != null)
                    cached = this.cache.get(idAndFilter + expanded + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.date != null && ur.date.equals(date)) {
                    sum += ur.res;
                } else {
                    ur.res = parseDOMToStats(parseXMLFileToDOM(xml));
                    ur.date = date;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(idAndFilter + expanded + key, ur));
                }
            } catch (Exception e) {
            }
        } else {
            try {
                String resource = url.toString().concat(r.getURI().toString());

                if (cache != null)
                    cached = this.cache.get(idAndFilter + r.getURI().toString() + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.date != null && ur.date.equals(date)) {
                    sum += ur.res;
                } else {
                    ur.res = parseDOMToStats(parseXMLFileToDOM(resource));
                    ur.date = date;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(idAndFilter + r.getURI().toString() + key, ur));
                }
            } catch (Exception e) {
            }
        }

        return sum;
    }

    private Document parseXMLFileToDOM(String request) {
        Document dom = null;
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");

        try {
            URL url = new URL(request);
            URLConnection conn = url.openConnection();
            if ("text/xml".equalsIgnoreCase(conn.getContentType())) {
                dom = builder.build(conn.getInputStream());
            }
        } catch (Exception e) {
        }

        return dom;
    }

    @SuppressWarnings("unchecked")
    private int parseDOMToStats(Document dom) {
        try {
            List<Element> elements, records, metrics;

            if ((elements = dom.getRootElement().getChildren()) == null)
                return -1;

            if ((records = elements.get(0).getChildren()) == null)
                return -1;

            if ((metrics = records.get(2).getChildren()) == null)
                return -1;

            return Integer.parseInt(metrics.get(0).getText());
        } catch (Exception e) {
            return 0;
        }
    }

    private String setIdAndFilter(String id) {
        String ids = "&ids=";
        String filters = "&filters=u:request_stem%3D~^/";
        // Use below in prod. Different now for testing.
        // String id =
        // RequestContext.getRequestContext().getRepository().getId();

        if (id.equals("www.uio.no"))
            return ids + "1" + filters + id;
        if (id.equals("www.hf.uio.no"))
            return ids + "2" + filters + id;
        if (id.equals("www.khm.uio.no"))
            return ids + "3" + filters + id;
        if (id.equals("www.odont.uio.no"))
            return ids + "4" + filters + id;
        if (id.equals("www.sv.uio.no"))
            return ids + "5" + filters + id;
        if (id.equals("www.tf.uio.no"))
            return ids + "6" + filters + id;
        if (id.equals("www.ub.uio.no"))
            return ids + "7" + filters + id;
        if (id.equals("www.uv.uio.no"))
            return ids + "8" + filters + id;
        if (id.equals("www.jus.uio.no"))
            return ids + "10" + filters + id;
        if (id.equals("www.uniforum.uio.no"))
            return ids + "11" + filters + id;
        if (id.equals("www.mn.uio.no"))
            return ids + "14" + filters + id;
        if (id.equals("www.med.uio.no"))
            return ids + "20" + filters + id;

        return null;
    }

    @Required
    public void setUrchinUser(String urchinUser) {
        this.urchinUser = urchinUser;
    }

    @Required
    public void setUrchinPassword(String urchinPassword) {
        this.urchinPassword = urchinPassword;
    }

    @Required
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cache = this.cacheManager.getCache("org.vortikal.URCHIN_CACHE");
    }

}
