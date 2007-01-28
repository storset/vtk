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
package org.vortikal.web.view.decorating.ssi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class SsiHandler implements InitializingBean {

    private static final Pattern INCLUDE_REGEXP = Pattern.compile(
            "<!--#include\\s+([\000-\377]*?)\\s*?=\"([\000-\377]*?)\"\\s*?-->",
            +Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private Map ssiProcessors = new HashMap();
    private static Log logger = LogFactory.getLog(SsiHandler.class);


    public String process(String content) {
        String docContentProcessed = content;
        int indexStartIncludeStatements;
        int indexEndIncludeStatements;
        StringBuffer sb;
        

        Matcher matcherInclude = INCLUDE_REGEXP.matcher(docContentProcessed);
        while (matcherInclude.find()) {            
            if (logger.isDebugEnabled()) {
                logger.debug("matcherInclude.group(0): " + matcherInclude.group(0));
            }
            indexStartIncludeStatements = matcherInclude.start();
            indexEndIncludeStatements = matcherInclude.end();
            String type = matcherInclude.group(1);
            String url = matcherInclude.group(2);
            //            String url = docContentProcessed.substring(matcherInclude.start(2), matcherInclude.end(2));
            SsiProcessor processor = (SsiProcessor) this.ssiProcessors.get(type);

            if (processor != null && url != null && !url.trim().equals("")) {
                String ssiContent = null;
                
                // if we can't fetch the included resource ssiContent is set to an empty String
                // and the SSI directive is removed by the replaceFirst statement bellow
                ssiContent = processor.resolve(url);

                // FIXME: Ugly processing all the way.. 
                if (ssiContent == null)
                    ssiContent = "";
                
                sb = new StringBuffer();        
                sb.append(docContentProcessed.substring(0, indexStartIncludeStatements));
                sb.append(ssiContent);
                sb.append(docContentProcessed.substring(indexEndIncludeStatements, docContentProcessed.length()));
                docContentProcessed = sb.toString();

                // We have to reset the matcher because we are using replaceFirst. From the
                // javadoc for Matcher:
                // "Invoking this method changes this matcher's state. If the matcher is to be
                // used in further matching operations then it should first be reset."
                matcherInclude.reset(docContentProcessed);
            }
        }
        return docContentProcessed;

    }


    public void afterPropertiesSet() throws Exception {
        logger.debug(this.ssiProcessors);
    }
    
    public void setSsiProcessors(Set set) {
        if (set == null) {
            return;
        }
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            SsiProcessor processor = (SsiProcessor) iter.next();
            String identifier = processor.getIdentifier();
            if (this.ssiProcessors.get(identifier) != null)
                throw new IllegalArgumentException("Processors canot have the same identifier");
            this.ssiProcessors.put(identifier, processor);
        }
    }
}
