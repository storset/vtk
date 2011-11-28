/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.web.actions.report;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Resource;
import org.vortikal.web.service.Service;

public class UrchinSearchReport extends AbstractReporter implements InitializingBean {
    private static final int maxResults = 50;

    private final Log logger = LogFactory.getLog(getClass());

    private CacheManager cacheManager;
    private Cache cache;
    private Element cached;

    private String name;
    private String viewName;

    private Service service;

    private String user;
    private String password;
    private String webHostName;
    private Map<String, Integer> urchinHostsToProfile;

    private static long fifteenDays = 86400000 * 15;

    private static final String VRTX_PARAM = "vrtx";
    private static final String SEARCHUIO_PARAM = "searchuio";
    private static final String SEARCH_PARAM = "search";
    private static final String QUERY_PARAM = "query";

    private static class UrchinSearchRes implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String sdate;
        public List<String> query;
        public List<Integer> hit;
    }

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());

        boolean recache = request.getParameter("recache") != null ? true : false;

        org.vortikal.web.service.URL resourceurl = service.constructURL(resource);
        org.vortikal.web.service.URL tmp;
        // TODO For prod:
        // if ((this.webHostName.equals("www.uio.no")) &&
        // (resource.getURI().toString().equals("/") ||
        // resource.getURI().toString().equals("/english")))
        if (resource.getURI().toString().equals("/") || resource.getURI().toString().equals("/english"))
            resourceurl.setParameter(VRTX_PARAM, SEARCHUIO_PARAM);
        else
            resourceurl.setParameter(VRTX_PARAM, SEARCH_PARAM);

        NumberFormat myFormat = NumberFormat.getInstance();
        myFormat.setMinimumIntegerDigits(2);

        /* Total */
        Calendar scal = Calendar.getInstance();
        scal.set(2011, 7, 1);
        Calendar ecal = Calendar.getInstance();

        String sdate = scal.get(Calendar.YEAR) + myFormat.format((scal.get(Calendar.MONTH)) + 1)
                + myFormat.format(scal.get(Calendar.DATE));
        String edate = ecal.get(Calendar.YEAR) + myFormat.format((ecal.get(Calendar.MONTH) + 1))
                + myFormat.format(ecal.get(Calendar.DATE));

        UrchinSearchRes usr = fetch(sdate, edate, "SearchTotal" + maxResults, token, resource, recache);
        List<org.vortikal.web.service.URL> url = new ArrayList<org.vortikal.web.service.URL>();
        List<String> title = new ArrayList<String>();
        if (usr != null) {
            for (int i = 0; i < usr.query.size(); i++) {
                title.add(usr.query.get(i));
                tmp = new org.vortikal.web.service.URL(resourceurl);
                tmp.addParameter(QUERY_PARAM, usr.query.get(i));
                url.add(tmp);
            }
            result.put("urlsTotal", url);
            result.put("titlesTotal", title);
            result.put("numbersTotal", usr.hit);
        }

        /* Thirty days */
        scal = ecal;

        // Need to do this twice because 86400000 * 30 = -1702967296.
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);

        sdate = scal.get(Calendar.YEAR) + myFormat.format((scal.get(Calendar.MONTH) + 1))
                + myFormat.format(scal.get(Calendar.DATE));

        usr = fetch(sdate, edate, "Search30" + maxResults, token, resource, recache);
        if (usr != null) {
            url = new ArrayList<org.vortikal.web.service.URL>();
            title = new ArrayList<String>();
            for (int i = 0; i < usr.query.size(); i++) {
                try {
                    title.add(usr.query.get(i));
                    tmp = new org.vortikal.web.service.URL(resourceurl);
                    tmp.addParameter(QUERY_PARAM, usr.query.get(i));
                    url.add(tmp);
                } catch (Exception e) {
                }
            }
            result.put("urlsThirty", url);
            result.put("titlesThirty", title);
            result.put("numbersThirty", usr.hit);
        }

        /* Sixty days */
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);

        sdate = scal.get(Calendar.YEAR) + myFormat.format((scal.get(Calendar.MONTH) + 1))
                + myFormat.format(scal.get(Calendar.DATE));

        usr = fetch(sdate, edate, "Search60" + maxResults, token, resource, recache);
        if (usr != null) {
            url = new ArrayList<org.vortikal.web.service.URL>();
            title = new ArrayList<String>();
            for (int i = 0; i < usr.query.size(); i++) {
                title.add(usr.query.get(i));
                tmp = new org.vortikal.web.service.URL(resourceurl);
                tmp.addParameter(QUERY_PARAM, usr.query.get(i));
                url.add(tmp);
            }
            result.put("urlsSixty", url);
            result.put("titlesSixty", title);
            result.put("numbersSixty", usr.hit);
        }

        return result;
    }

    private UrchinSearchRes fetch(String sdate, String edate, String key, String token, Resource resource,
            boolean recache) {
        UrchinSearchRes usr = null;
        // TODO For prod.
        // String uri = "/" + this,webHostName + resource.getURI().toString();
        String uri = "/www.uio.no" + resource.getURI().toString();
        if (!uri.endsWith("/"))
            uri += "/";
        uri += "index.html";

        try {
            if ((cache != null) && !recache)
                cached = this.cache.get(resource.getURI().toString() + key);
            else
                cached = null;

            if (cached != null)
                usr = (UrchinSearchRes) cached.getObjectValue();
            else
                usr = new UrchinSearchRes();

            if (usr.sdate != null && usr.sdate.equals(sdate)) {
                return usr;
            } else {
                URL url = new URL("https://statistikk.uio.no/session.cgi");

                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String data = URLEncoder.encode("user", "UTF-8") + "=" + URLEncoder.encode(user, "UTF-8");
                data += "&";
                data += URLEncoder.encode("pass", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
                data += "&";
                data += "app=" + URLEncoder.encode("admin", "UTF-8");
                data += "&";
                data += "action=" + URLEncoder.encode("login", "UTF-8");

                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.close();

                String sid = "";

                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    if (line.startsWith("var session =  \"sid=")) {
                        sid = line.substring(20, 41);
                        break;
                    }
                }

                if (sid.equals(""))
                    logger.warn("Could not get session id for urchin in " + this.getName() + ".");

                String surl = "https://statistikk.uio.no/session.cgi?";
                surl += "sid=" + sid;
                surl += "&app=urchin.cgi";
                surl += "&action=prop";
                Integer profileId;
                // TODO For prod:
                // if ((profileId = urchinHostsToProfile.get(this.webHostName))
                // == null)
                if ((profileId = urchinHostsToProfile.get("www.uio.no")) == null)
                    return null;
                surl += "&rid=" + profileId;
                surl += "&hl=en-US";
                surl += "&vid=1307";
                surl += "&bd=" + sdate;
                surl += "&ed=" + edate;
                surl += "&qt=" + uri + "|query|";
                surl += "&lv=2";
                surl += "&ns=10";
                surl += "&ss=0";
                surl += "&fd=";
                surl += "&ft=2";
                surl += "&sf=2";
                surl += "&sb=1";
                surl += "&sm=1";
                surl += "&dow=0";
                surl += "&dt=3";
                surl += "&dtc=2";
                surl += "&dcomp=0";
                surl += "&asid=";
                surl += "&xd=1";
                surl += "&x=7";

                logger.info("GET url in fetch: " + surl);

                url = new URL(surl);
                conn = (HttpsURLConnection) url.openConnection();

                List<String> queries = new ArrayList<String>();
                List<Integer> hits = new ArrayList<Integer>();
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                while ((line = rd.readLine()) != null) {
                    if (line.trim().startsWith("<ncols>")) {
                        break;
                    }
                }

                int count = 1;
                while ((line = rd.readLine()) != null) {
                    if (count > maxResults) {
                        break;
                    }

                    if (line.trim().startsWith("<record id=\"")) {
                        if ((line = rd.readLine()) != null && line.trim().startsWith("<name>")) {
                            queries.add(line.substring(line.indexOf('>') + 1, line.lastIndexOf('<')));
                        }
                        if ((line = rd.readLine()) != null && line.trim().startsWith("<value1>")) {
                            hits.add(Integer.parseInt(line.substring(line.indexOf('>') + 1, line.lastIndexOf('<'))));
                        }
                        rd.readLine();
                        count++;
                    }
                }

                rd.close();

                usr.sdate = sdate;
                usr.query = queries;
                usr.hit = hits;

                this.cache.put(new Element(resource.getURI().toString() + key, usr));

                return usr;
            }
        } catch (Exception warn) {
            logger.warn(warn.getMessage());
            return null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Required
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    @Required
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Required
    public void setService(Service service) {
        this.service = service;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
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
    }

}
