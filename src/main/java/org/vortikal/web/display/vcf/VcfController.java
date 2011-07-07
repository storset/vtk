/* Copyright (c) 2008, University of Oslo, Norway
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
package org.vortikal.web.display.vcf;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.util.codec.Base64;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.URL;

public class VcfController implements Controller {

    private PropertyTypeDefinition firstNamePropDef;
    private PropertyTypeDefinition surnamePropDef;
    private PropertyTypeDefinition usernamePropDef;
    private PropertyTypeDefinition positionPropDef;
    private PropertyTypeDefinition phonePropDef;
    private PropertyTypeDefinition mobilePropDef;
    private PropertyTypeDefinition faxPropDef;
    private PropertyTypeDefinition postalAddressPropDef;
    private PropertyTypeDefinition visitingAddressPropDef;
    private PropertyTypeDefinition emailPropDef;
    private PropertyTypeDefinition picturePropDef;
    private PropertyTypeDefinition thumbnailPropDef;
    private PropertyTypeDefinition imageWidthPropDef;
    private String maxImageWidth;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        RequestContext requestContext = RequestContext.getRequestContext();
        Repository repository = requestContext.getRepository();
        String token = requestContext.getSecurityToken();
        Path uri = requestContext.getResourceURI();
        URL requestURL = URL.create(request);

        Resource person = repository.retrieve(token, uri, true);

        String vcard = createVcard(person, repository, token, uri.getParent(), requestURL);
        if (vcard == null) {
            return null;
        }

        response.setContentType("text/x-vcard;charset=utf-8");
        String vcardFileName = getVcardFileName(person);
        response.setHeader("Content-Disposition", "filename=" + vcardFileName + ".vcf");
        ServletOutputStream out = response.getOutputStream();
        out.print(vcard);
        out.close();

        return null;
    }

    private String getVcardFileName(Resource person) {
        String name = person.getName();
        if (name.contains(".")) {
            name = name.substring(0, name.indexOf("."));
        }
        return name;
    }

    private String createVcard(Resource person, Repository repository, String token, Path currenturi, URL requestURL)
            throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCARD\n");
        sb.append("VERSION:3.0\n");

        if (getProperty(person, surnamePropDef) != null) {
            sb.append("N:" + getProp(person, surnamePropDef));
            if (getProperty(person, firstNamePropDef) != null)
                sb.append(";" + getProp(person, firstNamePropDef));
            sb.append("\n");
        } else if (getProperty(person, firstNamePropDef) != null)
            sb.append("N:" + getProp(person, firstNamePropDef) + "\n");
        else if (getProperty(person, usernamePropDef) != null)
            sb.append("N:" + getProp(person, usernamePropDef) + "\n");

        if (getProperty(person, firstNamePropDef) != null) {
            sb.append("FN:" + getProp(person, firstNamePropDef));
            if (getProperty(person, surnamePropDef) != null)
                sb.append(" " + getProp(person, surnamePropDef));
            sb.append("\n");
        } else if (getProperty(person, surnamePropDef) != null)
            sb.append("FN:" + getProp(person, surnamePropDef) + "\n");
        else if (getProperty(person, usernamePropDef) != null)
            sb.append("FN:" + getProp(person, usernamePropDef) + "\n");

        if (getProperty(person, positionPropDef) != null)
            sb.append("TITLE:" + getProp(person, positionPropDef) + "\n");

        if (getProperty(person, phonePropDef) != null)
            sb.append("TEL;TYPE=WORK,VOICE:" + getProp(person, phonePropDef) + "\n");

        if (getProperty(person, mobilePropDef) != null)
            sb.append("TEL;TYPE=CELL:" + getProp(person, mobilePropDef) + "\n");

        if (getProperty(person, faxPropDef) != null)
            sb.append("TEL;TYPE=FAX:" + getProp(person, faxPropDef) + "\n");

        /*
         * For data input reasons addresses has to be put in one (the street
         * address) field. ADR;TYPE=WORK:PO;Address Line 1;Address Line
         * 2;City;Province;PostalCode;Country could however be used if this
         * should change in the future.
         */
        if (getProperty(person, postalAddressPropDef) != null)
            sb.append("ADR;TYPE=WORK,POSTAL:;;" + getProp(person, postalAddressPropDef) + ";;;;\n");

        if (getProperty(person, visitingAddressPropDef) != null)
            sb.append("ADR;TYPE=WORK:;;" + getProp(person, visitingAddressPropDef) + ";;;;\n");

        if (getProperty(person, emailPropDef) != null)
            sb.append("EMAIL;TYPE=INTERNET:" + getProp(person, emailPropDef) + "\n");

        if (getProperty(person, picturePropDef) != null) {
            String pic = b64Thumbnail(person, repository, token, currenturi, requestURL);
            if (pic != null)
                sb.append(pic);
        }

        sb.append("REV:" + getDtstamp() + "\n");
        sb.append("END:VCARD");
        return sb.toString();
    }

    private String getProp(Resource person, PropertyTypeDefinition propDef) {
        return getProperty(person, propDef).getFormattedValue();
    }

    private Property getProperty(Resource person, PropertyTypeDefinition propDef) {
        Property prop = person.getProperty(propDef);
        if (prop == null) {
            prop = person.getProperty(Namespace.STRUCTURED_RESOURCE_NAMESPACE, propDef.getName());
        }

        return prop;
    }

    private String getDtstamp() {
        String dateFormat = "yyyyMMdd'T'HHmmss'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(Calendar.getInstance().getTime());
    }

    private String b64Thumbnail(Resource person, Repository repository, String token, Path currenturi, URL requestURL)
            throws Exception {
        String path = getProp(person, picturePropDef);

        Path p = null;

        try {
            URL pURL = URL.parse(path);
            if (requestURL.getHost().equals(pURL.getHost()))
                p = pURL.getPath();
        } catch (Exception e) {
        }

        try {
            if (!path.startsWith("/"))
                p = currenturi.extend(path);
            else
                p = Path.fromString(path);
        } catch (Exception e) {
        }

        if (p == null)
            return null;

        Resource r = repository.retrieve(token, p, true);
        Property thumbnail = r.getProperty(thumbnailPropDef);
        InputStream i;

        if (thumbnail == null) {
            int width = getProperty(r, imageWidthPropDef).getIntValue();

            if (width > Integer.parseInt(maxImageWidth))
                return null;

            i = repository.getInputStream(token, p, true);
        } else
            i = thumbnail.getBinaryStream().getStream();

        /* Base64 encodes the thumbnail. */
        String encoded = Base64.encode(i);
        String output = "";

        /*
         * Base64 encoding in vCards needs to be 75 characters on each line,
         * starting with a white space.
         */
        int j = 0, k = 76, len = encoded.length();
        while (k < len) {
            output += "\n  " + encoded.substring(j, k);
            j = k;
            k += 76;
        }
        output += "\n  " + encoded.substring(j, len);

        return "PHOTO;BASE64:" + output + "\n";
    }

    @Required
    public void setFirstNamePropDef(PropertyTypeDefinition firstNamePropDef) {
        this.firstNamePropDef = firstNamePropDef;
    }

    @Required
    public void setSurnamePropDef(PropertyTypeDefinition surnamePropDef) {
        this.surnamePropDef = surnamePropDef;
    }

    @Required
    public void setUsernamePropDef(PropertyTypeDefinition usernamePropDef) {
        this.usernamePropDef = usernamePropDef;
    }

    @Required
    public void setPositionPropDef(PropertyTypeDefinition positionPropDef) {
        this.positionPropDef = positionPropDef;
    }

    @Required
    public void setPhonePropDef(PropertyTypeDefinition phonePropDef) {
        this.phonePropDef = phonePropDef;
    }

    @Required
    public void setMobilePropDef(PropertyTypeDefinition mobilePropDef) {
        this.mobilePropDef = mobilePropDef;
    }

    @Required
    public void setFaxPropDef(PropertyTypeDefinition faxPropDef) {
        this.faxPropDef = faxPropDef;
    }

    @Required
    public void setPostalAddressPropDef(PropertyTypeDefinition postalAddressPropDef) {
        this.postalAddressPropDef = postalAddressPropDef;
    }

    @Required
    public void setVisitingAddressPropDef(PropertyTypeDefinition visitingAddressPropDef) {
        this.visitingAddressPropDef = visitingAddressPropDef;
    }

    @Required
    public void setEmailPropDef(PropertyTypeDefinition emailPropDef) {
        this.emailPropDef = emailPropDef;
    }

    @Required
    public void setPicturePropDef(PropertyTypeDefinition picturePropDef) {
        this.picturePropDef = picturePropDef;
    }

    @Required
    public void setThumbnailPropDef(PropertyTypeDefinition thumbnailPropDef) {
        this.thumbnailPropDef = thumbnailPropDef;
    }

    @Required
    public void setImageWidthPropDef(PropertyTypeDefinition imageWidthPropDef) {
        this.imageWidthPropDef = imageWidthPropDef;
    }

    @Required
    public void setMaxImageWidth(String maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }
}
