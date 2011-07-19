/* Copyright (c) 2011, University of Oslo, Norway
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
package org.vortikal.repository.content;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import net.sf.json.JSONObject;

import org.vortikal.repository.store.MetadataImpl;
import org.vortikal.repository.store.VideoMetadata;

public class ExternalProgramVideoContentFactory implements ContentFactory {
    // TODO: cache on fail?
    private String programLocation = null;
    private String repositoryDataDirectory;

    @Override
    public Class<?>[] getRepresentationClasses() {
        return new Class<?>[] { VideoMetadata.class };
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getContentRepresentation(Class<?> clazz, InputStreamWrapper content) throws Exception {

        if (programLocation == null || "".equals(programLocation)) {
            return null;
        }

        if (content.getPath() == null) {
            return null;
        }

        String jsonResult = null;
        ExternalProgramThread t = new ExternalProgramThread(content.getPath().toString());
        t.start();
        t.join(10000); // wait for 10 sec
        if (t.isAlive()) { // we have waited long enough - time to kill
            t.destroyProc();
            t.interrupt();
        } else {
            jsonResult = t.getResult();
        }
        t = null;

        if (jsonResult != null) {
            JSONObject obj = JSONObject.fromObject(jsonResult);
            Iterator<String> i = obj.keys();
            MetadataImpl metdata = new MetadataImpl();
            while (i.hasNext()) {
                String key = i.next();
                metdata.addAttributeValue(key, obj.getString(key));
            }
            return metdata;
        }
        return null;
    }

    public String getProgramLocation() {
        return programLocation;
    }

    public void setProgramLocation(String programLocation) {
        this.programLocation = programLocation;
    }

    public void setRepositoryDataDirectory(String repositoryDataDirectory) {
        this.repositoryDataDirectory = repositoryDataDirectory;
    }

    public String getRepositoryDataDirectory() {
        return repositoryDataDirectory;
    }

    /*
     * This thread is only used to ensure that the java VM will not break if the
     * external process hangs for whatever reason. We only us the result if the
     * thread is no longer running.
     */
    private class ExternalProgramThread extends Thread {

        String result = null;
        Process process = null;
        String localPath = null;

        ExternalProgramThread(String localPath) {
            this.localPath = localPath;
        }

        public void run() {
            try {
                Runtime runtime = Runtime.getRuntime();
                String cmd[] = new String[] { getProgramLocation(), localPath };

                process = runtime.exec(cmd);
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line;
                result = "";
                while ((line = br.readLine()) != null) {
                    result += line;
                }
                is.close();
            } catch (Exception e) {
                // ignore
            }

        }

        /*
         * Get the result if available - remember to check that the thread has
         * exited before getting the result
         */
        String getResult() {
            return result;
        }

        /* Destroys the external process if it is running */
        public void destroyProc() {
            if (process != null) {
                process.destroy();
            }
        }

    }

}
