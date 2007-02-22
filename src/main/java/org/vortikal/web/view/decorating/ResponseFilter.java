package org.vortikal.web.view.decorating;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.web.servlet.BufferedResponse;

public interface ResponseFilter {

    public void process(Map model, HttpServletRequest request, BufferedResponse bufferedResponse) throws IOException;

}
