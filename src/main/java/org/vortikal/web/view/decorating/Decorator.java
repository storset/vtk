package org.vortikal.web.view.decorating;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.web.servlet.BufferedResponse;

public interface Decorator {

    public void decorate(Map model, HttpServletRequest request,
            BufferedResponse bufferedResponse) throws Exception,
            UnsupportedEncodingException, IOException;

}