package org.vortikal.repository.content;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.store.MetadataImpl;

public abstract class AbstractExternalProgramContentFactory {
    
    // TODO: cache on fail?
    private String programLocation;
    private static Log logger = LogFactory.getLog(AbstractExternalProgramContentFactory.class.getName());
    
    @SuppressWarnings("unchecked")
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
            logger.error("Killed thread. Error Cmd: " + t.getCmd());
            logger.error("Result befor kill:" + t.getResult());
        } else {
            jsonResult = t.getResult();
        }

        if (jsonResult != null) {
            
            JSONObject obj = JSONObject.fromObject(jsonResult);
            if(obj.isEmpty()){
                logger.debug("Debug Cmd: " + t.getCmd());
                logger.debug("JSON object is empthy. Problems with pharsing: " + jsonResult);
            }
            Iterator<String> i = obj.keys();
            MetadataImpl metdata = new MetadataImpl();
            while (i.hasNext()) {
                String key = i.next();
                metdata.addAttributeValue(key, obj.getString(key));
            }
            return metdata;
        }else{
            logger.debug("No metadata extracted. Debug Cmd: " + t.getCmd());
        }
        return null;
    }

    public String getProgramLocation() {
        return programLocation;
    }

    public void setProgramLocation(String programLocation) {
        this.programLocation = programLocation;
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
        private String cmd[] = null;

        ExternalProgramThread(String localPath) {
            this.localPath = localPath;
        }

        public void run() {
            try {
                Runtime runtime = Runtime.getRuntime();
                cmd = new String[] { getProgramLocation(), localPath };

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

        public String[] getCmd() {
            return cmd;
        }

    }

}
