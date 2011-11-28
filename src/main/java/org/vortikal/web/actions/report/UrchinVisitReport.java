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
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;

public class UrchinVisitReport extends AbstractReporter implements InitializingBean {
    // TODO 50 in prod.
    private static final int maxResults = 5;

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

    private static class UrchinVisitRes implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public String sdate;
        public String edate;
        public List<String> uri;
        public List<Integer> visit;
    }

    @Override
    public Map<String, Object> getReportContent(String token, Resource resource, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("reportname", this.getName());
        Principal p = SecurityContext.getSecurityContext().getPrincipal();
        Repository repo = RequestContext.getRequestContext().getRepository();
        Resource r;

        boolean recache = request.getParameter("recache") != null ? true : false;

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

        UrchinVisitRes uvr = fetch(sdate, edate, "VisitTotal" + maxResults, token, resource, recache);
        List<org.vortikal.web.service.URL> url = new ArrayList<org.vortikal.web.service.URL>();
        List<String> title = new ArrayList<String>();
        for (int i = 0; i < uvr.uri.size(); i++) {
            try {
                r = repo.retrieve(token, Path.fromString(uvr.uri.get(i)), false);
                title.add(r.getTitle());
                url.add(service.constructURL(r, p));
            } catch (Exception e) {
            }
        }
        result.put("urlsTotal", url);
        result.put("titlesTotal", title);
        result.put("numbersTotal", uvr.visit);

        /* Thirty days */
        scal = ecal;

        // Need to do this twice because 86400000 * 30 = -1702967296.
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);

        sdate = scal.get(Calendar.YEAR) + myFormat.format((scal.get(Calendar.MONTH) + 1))
                + myFormat.format(scal.get(Calendar.DATE));

        uvr = fetch(sdate, edate, "Visit30" + maxResults, token, resource, recache);
        url = new ArrayList<org.vortikal.web.service.URL>();
        title = new ArrayList<String>();
        for (int i = 0; i < uvr.uri.size(); i++) {
            try {
                r = repo.retrieve(token, Path.fromString(uvr.uri.get(i)), false);
                title.add(r.getTitle());
                url.add(service.constructURL(r, p));
            } catch (Exception e) {
            }
        }
        result.put("urlsThirty", url);
        result.put("titlesThirty", title);
        result.put("numbersThirty", uvr.visit);

        /* Sixty days */
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);
        scal.setTimeInMillis(scal.getTimeInMillis() - fifteenDays);

        sdate = scal.get(Calendar.YEAR) + myFormat.format((scal.get(Calendar.MONTH) + 1))
                + myFormat.format(scal.get(Calendar.DATE));

        uvr = fetch(sdate, edate, "Visit60" + maxResults, token, resource, recache);
        url = new ArrayList<org.vortikal.web.service.URL>();
        title = new ArrayList<String>();
        for (int i = 0; i < uvr.uri.size(); i++) {
            try {
                r = repo.retrieve(token, Path.fromString(uvr.uri.get(i)), false);
                title.add(r.getTitle());
                url.add(service.constructURL(r, p));
            } catch (Exception e) {
            }
        }
        result.put("urlsSixty", url);
        result.put("titlesSixty", title);
        result.put("numbersSixty", uvr.visit);

        return result;
    }

    private UrchinVisitRes fetch(String sdate, String edate, String key, String token, Resource resource,
            boolean recache) {
        UrchinVisitRes uvr;
        Repository repo = RequestContext.getRequestContext().getRepository();
        // TODO For prod.
        // String uri = "/" + repo.getId() + resource.getURI().toString();
        String uri = "/www.uio.no" + resource.getURI().toString();

        try {
            if ((cache != null) && !recache)
                cached = this.cache.get(resource.getURI().toString() + key);
            else
                cached = null;

            if (cached != null)
                uvr = (UrchinVisitRes) cached.getObjectValue();
            else
                uvr = new UrchinVisitRes();

            if ((uvr.edate != null && uvr.edate.equals(edate)) && (uvr.sdate != null && uvr.sdate.equals(sdate))) {
                return uvr;
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
                surl += "&vid=1304";
                surl += "&bd=" + sdate;
                surl += "&ed=" + edate;
                surl += "&ns=10";
                surl += "&ss=0";
                surl += "&fd=" + uri;
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

                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                List<String> uris = new ArrayList<String>();
                List<Integer> visits = new ArrayList<Integer>();
                // TODO For prod:
                // int id = ("/" + repo.getId()).length();
                int id = ("/" + "www.uio.no").length();
                int count = 1;
                String tmp = "";
                Resource r;
                while ((line = rd.readLine()) != null) {
                    if (line.trim().startsWith("<ncols>")) {
                        break;
                    }
                }
                while ((line = rd.readLine()) != null) {
                    if (count > maxResults) {
                        break;
                    }

                    if (line.trim().startsWith("<record id=\"")) {
                        if ((line = rd.readLine()) != null && line.trim().startsWith("<name>")) {
                            tmp = line.substring(line.indexOf('/') + id, line.lastIndexOf('<'));

                            try {
                                r = repo.retrieve(token, Path.fromString(tmp), false);
                                uris.add(r.getURI().toString());
                                if ((line = rd.readLine()) != null && line.trim().startsWith("<value1>")) {
                                    visits.add(Integer.parseInt(line.substring(line.indexOf('>') + 1, line
                                            .lastIndexOf('<'))));
                                }
                                count++;
                            } catch (Exception e) {
                                r = null;
                            }

                            if (r == null && tmp.endsWith("index.html")) {
                                try {
                                    r = repo.retrieve(token, Path.fromString(tmp.substring(0, tmp.lastIndexOf('/'))),
                                            false);
                                    uris.add(r.getURI().toString());
                                    if ((line = rd.readLine()) != null && line.trim().startsWith("<value1>")) {
                                        visits.add(Integer.parseInt(line.substring(line.indexOf('>') + 1, line
                                                .lastIndexOf('<'))));
                                    }
                                    count++;
                                } catch (Exception e) {
                                }
                            }

                            while ((line = rd.readLine()) != null) {
                                if (line.trim().equals("</record>")) {
                                    break;
                                }
                            }
                        }
                    }
                }

                rd.close();

                uvr.sdate = sdate;
                uvr.edate = edate;
                uvr.uri = uris;
                uvr.visit = visits;

                this.cache.put(new Element(resource.getURI().toString() + key, uvr));

                return uvr;
            }
        } catch (Exception e) {
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
    public void setService(Service service) {
        this.service = service;
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
