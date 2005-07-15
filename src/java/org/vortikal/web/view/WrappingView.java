package org.vortikal.web.view;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

public interface WrappingView extends View {

    public void renderView(View view, Map model, HttpServletRequest request, HttpServletResponse response) throws Exception;
}
