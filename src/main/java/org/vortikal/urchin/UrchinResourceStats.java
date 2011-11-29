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
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;

public class UrchinResourceStats implements InitializingBean {
    private final Log logger = LogFactory.getLog(getClass());

    private String urchinUser;
    private String urchinPassword;
    private String webHostName;
    private Map<String, Integer> urchinHostsToProfile;

    private CacheManager cacheManager;
    private Cache cache;
    private net.sf.ehcache.Element cached;
    private UrchinRes ur;

    private static long twentyDays = 86400000 * 20;
    private static long fifteenDays = 86400000 * 15;

    private static class UrchinRes implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String edate;
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

    public int[] months(Resource r, String token, boolean recache) {
        Calendar urchinStartCal = Calendar.getInstance();
        urchinStartCal.set(2011, 7, 1);

        int[] months = new int[13];
        boolean today = true;

        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 12; i++) {
            if (cal.get(Calendar.MONTH) < urchinStartCal.get(Calendar.MONTH))
                break;

            String sdate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-"
                    + cal.getActualMinimum(Calendar.DATE);

            String edate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-";

            if (today) {
                edate += cal.get(Calendar.DATE);
                today = false;
            } else
                edate += cal.getActualMaximum(Calendar.DATE);

            months[cal.get(Calendar.MONTH)] = fetch(r, token, sdate, edate, "" + cal.get(Calendar.MONTH), recache);
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

    public int thirtyTotal(Resource r, String token, boolean recache) {
        Calendar ecal = Calendar.getInstance();
        Calendar scal = Calendar.getInstance();

        // Need to do this twice because 86400000 * 30 = -1702967296.
        scal.setTimeInMillis(ecal.getTimeInMillis() - fifteenDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);

        String sdate = scal.get(Calendar.YEAR) + "-" + (scal.get(Calendar.MONTH) + 1) + "-" + scal.get(Calendar.DATE);
        String edate = ecal.get(Calendar.YEAR) + "-" + (ecal.get(Calendar.MONTH) + 1) + "-" + ecal.get(Calendar.DATE);

        return fetch(r, token, sdate, edate, "thirty", recache);
    }

    public int sixtyTotal(Resource r, String token, boolean recache) {
        Calendar ecal = Calendar.getInstance();
        Calendar scal = Calendar.getInstance();

        // Need to do this three times.
        scal.setTimeInMillis(ecal.getTimeInMillis() - twentyDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - twentyDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - twentyDays);

        String sdate = scal.get(Calendar.YEAR) + "-" + (scal.get(Calendar.MONTH) + 1) + "-" + scal.get(Calendar.DATE);
        String edate = ecal.get(Calendar.YEAR) + "-" + (ecal.get(Calendar.MONTH) + 1) + "-" + ecal.get(Calendar.DATE);

        return fetch(r, token, sdate, edate, "sixty", recache);
    }

    public int visitsTotal(Resource r, String token, boolean recache) {
        Calendar urchinStartCal = Calendar.getInstance();
        urchinStartCal.set(2011, 7, 1);
        String urchinStartDate = urchinStartCal.get(Calendar.YEAR) + "-" + (urchinStartCal.get(Calendar.MONTH) + 1)
                + "-" + urchinStartCal.get(Calendar.DATE);

        Calendar today = Calendar.getInstance();
        String todayDate = today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-"
                + today.get(Calendar.DATE);

        return fetch(r, token, urchinStartDate, todayDate, "visitsTotal", recache);
    }

    public int pagesTotal(Resource r, String token, boolean recache) {
        Calendar urchinStartCal = Calendar.getInstance();
        urchinStartCal.set(2011, 7, 1);
        String urchinStartDate = urchinStartCal.get(Calendar.YEAR) + "-" + (urchinStartCal.get(Calendar.MONTH) + 1)
                + "-" + urchinStartCal.get(Calendar.DATE);

        Calendar today = Calendar.getInstance();
        String todayDate = today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-"
                + today.get(Calendar.DATE);

        return fetch(r, token, urchinStartDate, todayDate, "pagesTotal", recache);
    }

    private int fetch(Resource r, String token, String sdate, String edate, String key, boolean recache) {
        int sum = 0;

        if (urchinUser.equals("") || urchinPassword.equals(""))
            return sum;

        String url = "https://statistikk.uio.no/services/v2/reportservice/data";
        String login = "?login=" + urchinUser + "&password=" + urchinPassword;
        String parameters = "&start-date=" + sdate + "&end-date=" + edate;
        parameters += "&dimensions=u:request_stem";
        if (key.equals("pagesTotal"))
            parameters += "&metrics=u:pages";
        else
            parameters += "&metrics=u:visits";
        parameters += "&table=12";
        Integer profileId;
        // TODO For prod:
        // if ((profileId = urchinHostsToProfile.get(this.webHostName)) == null)
        if ((profileId = urchinHostsToProfile.get("www.uio.no")) == null)
            return sum;
        parameters += "&ids=" + profileId;
        // TODO For prod:
        // parameters += "&filters=u:request_stem%3D~^/" + this.webHostName;
        parameters += "&filters=u:request_stem%3D~^/" + "www.uio.no";

        if (r.isCollection()) {
            try {
                String expanded = r.getURI().expand("index.html").toString();
                String html = url + login + parameters + expanded;

                if ((cache != null) && !recache)
                    cached = this.cache.get(expanded + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.edate != null && ur.edate.equals(edate)) {
                    sum += ur.res;
                } else {
                    logger.info("GET url in fetch: " + url + "?login=X&password=X" + parameters + expanded);
                    ur.res = parseDOMToStats(parseXMLFileToDOM(html));
                    ur.edate = edate;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(expanded + key, ur));
                }
            } catch (Exception warn) {
                logger.warn(warn.getMessage());
            }

            try {
                String expanded = r.getURI().expand("index.xml").toString();
                String xml = url + login + parameters + expanded;

                if ((cache != null) && !recache)
                    cached = this.cache.get(expanded + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.edate != null && ur.edate.equals(edate)) {
                    sum += ur.res;
                } else {
                    logger.info("GET url in fetch: " + url + "?login=X&password=X" + parameters + expanded);
                    ur.res = parseDOMToStats(parseXMLFileToDOM(xml));
                    ur.edate = edate;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(expanded + key, ur));
                }
            } catch (Exception warn) {
                logger.warn(warn.getMessage());
            }
        } else {
            try {
                String uri = r.getURI().toString();
                String resource = url + login + parameters + uri;

                if ((cache != null) && !recache)
                    cached = this.cache.get(r.getURI().toString() + key);
                else
                    cached = null;

                if (cached != null)
                    ur = (UrchinRes) cached.getObjectValue();
                else
                    ur = new UrchinRes();

                if (ur.edate != null && ur.edate.equals(edate)) {
                    sum += ur.res;
                } else {
                    logger.info("GET url in fetch: " + url + "?login=X&password=X" + parameters + uri);
                    ur.res = parseDOMToStats(parseXMLFileToDOM(resource));
                    ur.edate = edate;
                    sum += ur.res;
                    if (cache != null)
                        this.cache.put(new net.sf.ehcache.Element(r.getURI().toString() + key, ur));
                }
            } catch (Exception warn) {
                logger.warn(warn.getMessage());
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
        } catch (Exception warn) {
            logger.warn("parseXMLFileToDOM: " + warn.getMessage() + " GET: " + request);
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
        } catch (Exception ignore) {
            logger.debug("parseDOMToStats: " + ignore.getMessage());
            return 0;
        }
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

    @Required
    public void setUrchinHostsToProfile(Map<String, Integer> urchinHostsToProfile) {
        this.urchinHostsToProfile = urchinHostsToProfile;
    }

    @Required
    public void setWebHostName(String webHostName) {
        String[] names = webHostName.trim().split("\\s*,\\s*");
        for (String name : names) {
            if ("*".equals(name)) {
                this.webHostName = "localhost";
                return; // Use default value
            }
        }
        this.webHostName = names[0];
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.cache = this.cacheManager.getCache("org.vortikal.URCHIN_CACHE");
        if (cache == null)
            logger.warn("Cache is null.");
    }

}
