package org.vortikal.web.decorating.components;

import java.util.Map;

import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class IncludeMediaPlayerComponent extends ResourceMediaPlayerComponent {

    private final static String PARAMETER_URL = "url";
    private final static String PARAMETER_HEIGHT = "height";
    private final static String PARAMETER_WIDTH = "width";
    private final static String PARAMETER_AUTOPLAY = "autoplay";
    private final static String PARAMETER_CONTENT_TYPE = "content-type";

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String url = request.getStringParameter(PARAMETER_URL);
        String height = request.getStringParameter(PARAMETER_HEIGHT);
        String width = request.getStringParameter(PARAMETER_WIDTH);
        String autoplay = request.getStringParameter(PARAMETER_AUTOPLAY);
        String contentType = request.getStringParameter(PARAMETER_CONTENT_TYPE);

        createLocalUrlToMediaFile(request.getServletRequest(), url, model);

        // Overwrites default values
        if (height != null)
            model.put("height", height);
        if (width != null)
            model.put("width", width);
        if (autoplay != null)
            model.put("autoplay", autoplay);

        String extension = getExtension(url);
        if (contentType != null) {
            model.put("contentType", contentType);
        } else if (extentionToMimetype.containsKey(extension)) {
            model.put("contentType", extentionToMimetype.get(extension));
        }

        model.put("extension", extension);
    }

}
