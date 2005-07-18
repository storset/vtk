package org.vortikal.web.view.wrapper;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

public interface ViewWrapper {

    public void renderView(View view, Map model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
