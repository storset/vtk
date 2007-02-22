package org.vortikal.web.view.decorating;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.web.servlet.BufferedResponse;
import org.vortikal.web.view.decorating.ssi.SsiHandler;

public class ResponseFilterImpl implements ResponseFilter {

    private SsiHandler handler;
    
    public void process(Map model, HttpServletRequest request,
            BufferedResponse bufferedResponse) throws IOException {
        if (this.handler != null) {
            String content = null;
            try {
                content = new String(bufferedResponse.getContentBuffer(), bufferedResponse.getCharacterEncoding());
            } catch (UnsupportedEncodingException e) {
                return;
            }
            
            String result = this.handler.process(content);
            
            if (content.equals(result))
                return;
            
            bufferedResponse.resetBuffer();
            PrintWriter writer = bufferedResponse.getWriter();
            writer.write(result);
        }
        return;
    }

    public void setHandler(SsiHandler handler) {
        this.handler = handler;
    }

}
