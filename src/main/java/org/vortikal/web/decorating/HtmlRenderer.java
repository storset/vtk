package org.vortikal.web.decorating;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.View;

public interface HtmlRenderer extends View {

    @SuppressWarnings("unchecked")
    public HtmlPageContent render(Map model, HttpServletRequest request) throws Exception;

}
