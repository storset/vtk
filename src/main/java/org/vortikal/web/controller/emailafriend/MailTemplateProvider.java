/* Copyright (c) 2005, 2008 University of Oslo, Norway
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

package org.vortikal.web.controller.emailafriend;

public class MailTemplateProvider {
	
	public String generateMailBody(String title, String articleURI, String mailFrom, String comment,
			String serverHostname, String serverHostnameShort, int serverPort, String language) {
		
		StringBuilder sb = new StringBuilder();
		
		if (language.equals("no_NO")) {
			
			sb.append("Hei!\n\n");
			
			sb.append(serverHostnameShort + " har en artikkel jeg tror kan være interessant for deg:\n");
			
			sb.append(title + "\n\n");
			
			sb.append(comment + "\n\n");
			
			sb.append("Les hele artikkelen her: \n");
			if (serverPort != 80) {
				sb.append("http://" + serverHostname + ":" + serverPort + articleURI + " \n\n");
			} else {
				sb.append("http://" + serverHostname + articleURI + " \n\n");
			}
			
			sb.append("Med vennlig hilsen,\n");
			
			sb.append(mailFrom + "\n\n\n\n");
			
			sb.append("--------------------------------------------\n");
			sb.append("Denne meldingen er sendt på oppfordring fra " + mailFrom + "\n\n");
			sb.append("Din e-post adresse blir ikke lagret.\n");
			sb.append("Du vil ikke motta flere meldinger av denne typen,\n");
			sb.append("med mindre noen tipser deg om andre artikler på " + serverHostname + "/");
			
		} else if (language.equals("no_NO_NY")) {
			
			sb.append("Hei!\n\n");
			
			sb.append(serverHostnameShort + " har en artikkel eg trur kan væra interessant for deg:\n");
			
			sb.append(title + "\n\n");
			
			sb.append(comment + "\n\n");
			
			sb.append("Les heile artikkelen her: \n");
			if (serverPort != 80) {
				sb.append("http://" + serverHostname + ":" + serverPort + articleURI + " \n\n");
			} else {
				sb.append("http://" + serverHostname + articleURI + " \n\n");
			}
			
			sb.append("Med vennleg helsing,\n");
			
			sb.append(mailFrom + "\n\n\n\n");
			
			sb.append("--------------------------------------------\n");
			sb.append("Denne meldinga er sendt på oppfordring frå " + mailFrom + "\n\n");
			sb.append("Din e-post adresse blir ikkje lagra.\n");
			sb.append("Du vil ikkje motta fleire meldingar som dette,\n");
			sb.append("med mindre nokon tipsar deg om andre artiklar på " + serverHostname + "/");
			
			// language=en or default
		} else {
			
			sb.append("Hi!\n\n");
			
			sb.append(serverHostnameShort + " have an article I think you will find interesting:\n");
			
			sb.append(title + "\n\n");
			
			sb.append(comment + "\n\n");
			
			sb.append("Read the entire article here: \n");
			if (serverPort != 80) {
				sb.append("http://" + serverHostname + ":" + serverPort + articleURI + " \n\n");
			} else {
				sb.append("http://" + serverHostname + articleURI + " \n\n");
			}
			
			sb.append("Best regards,\n");
			
			sb.append(mailFrom + "\n\n\n\n");
			
			sb.append("--------------------------------------------\n");
			sb.append("This message is sent on  from " + mailFrom + "\n\n");
			sb.append("Your emailaddress will not be saved.\n");
			sb.append("You will not recieve more messages of this type,\n");
			sb.append("unless someone tip you of other articles " + serverHostname + "/");
			
		}
		
		return sb.toString();
	}
}
