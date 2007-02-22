package org.vortikal.web.view.decorating;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vortikal.web.servlet.BufferedResponse;

public interface Decorator {

    public void decorate(Map model, HttpServletRequest request,
            BufferedResponse bufferedResponse,
            HttpServletResponse servletResponse) throws Exception,
            UnsupportedEncodingException, IOException;

}