/* Copyright (c) 2006, University of Oslo, Norway
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
package org.vortikal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import no.uio.usit.miyagi.BeanOverloadException;
import no.uio.usit.miyagi.Miyagi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class DependencyInjectionSpringStringContextTestsSpike 
    extends AbstractDependencyInjectionSpringStringContextTests {

    private static final Log LOG = LogFactory.getLog(DependencyInjectionSpringStringContextTestsSpike.class);
    
    protected String getConfigAsString() {
        LOG.debug("getConfigAsString()");
        String configAsString = null;

        try {
            List<File> fileList = findAllXmlFiles(new File("target/classes/vortikal/beans/vhost/"));
            fileList.add(new File("target/vortikal/WEB-INF/applicationContext.xml"));
            
            
            Miyagi m = new Miyagi(fileList);
            m.setBeanOverloadingAllowed(true);
            m.buildBeans();

            List<String> beanIdList = new ArrayList<String>();
            beanIdList.add("propertyConfigurer");
            beanIdList.add("collectionListingAsFeedView");
            beanIdList.add("repository.contentStore");
            beanIdList.add("localPrincipalStore");
            beanIdList.add("systemUsersGroup");
            beanIdList.add("repository.cache");

            configAsString = m.getXml(beanIdList);
        } catch (BeanOverloadException e) {
            // found overloaded bean (not interesting in this context)
            LOG.debug("overloaded bean found: ", e);
        }

        return configAsString;
    }
    
    public void testSpike() {
        assertTrue(true);
    }

    /**
     * Find all xml-files under given dir (pDir)
     * @param pDir given dir
     * @return List of files
     */
    private List<File> findAllXmlFiles(File pDir) {
        List<File> fileList = new ArrayList<File>();

        visitAllFiles(pDir, fileList);

        return fileList;
    }

    /**
     * Put all files under dir in <code>_xmlFiles</code>.
     * This method is recursive.
     *
     * @param dir
     */
    private void visitAllFiles(File dir, List<File> pFileList) {
        if (dir.isDirectory()) {
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {
                visitAllFiles(new File(dir, children[i]), pFileList);
            }
        } else {
            try {
                if (dir.getCanonicalPath().endsWith(".xml")
                        && !dir.getCanonicalPath().endsWith("decorators.xml")) {
                    pFileList.add(dir);
//                    LOG.debug(dir);
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

}
