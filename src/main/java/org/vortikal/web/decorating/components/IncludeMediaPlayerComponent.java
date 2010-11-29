package org.vortikal.web.decorating.components;

import java.util.LinkedHashMap;
import java.util.Map;

import org.vortikal.repository.Path;
import org.vortikal.repository.Resource;
import org.vortikal.security.SecurityContext;
import org.vortikal.web.decorating.DecoratorRequest;
import org.vortikal.web.decorating.DecoratorResponse;

public class IncludeMediaPlayerComponent extends ResourceMediaPlayerComponent {

    private final static String PARAMETER_URL = "url";
    private final static String PARAMETER_URL_DESCRIPTION = "Media file url or uri";
    private final static String PARAMETER_HEIGHT = "height";
    private final static String PARAMETER_HEIGHT_DESCRIPTION = "Height of player";
    private final static String PARAMETER_WIDTH = "width";
    private final static String PARAMETER_WIDTH_DESCRIPTION = "Width of player";
    private final static String PARAMETER_AUTOPLAY = "autoplay";
    private final static String PARAMETER_AUTOPLAY_DESCRIPTION = "Start playing immediately if set to 'true'. Default is 'false'";
    private final static String PARAMETER_CONTENT_TYPE = "content-type";
    private final static String PARAMETER_CONTENT_TYPE_DESCRIPTION = "Content type of media file";

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String url = request.getStringParameter(PARAMETER_URL);
        String height = request.getStringParameter(PARAMETER_HEIGHT);
        String width = request.getStringParameter(PARAMETER_WIDTH);
        String autoplay = request.getStringParameter(PARAMETER_AUTOPLAY);
        String contentType = request.getStringParameter(PARAMETER_CONTENT_TYPE);

        createLocalUrlToMediaFile(url, model);

        // Overwrites default values
        if (height != null && !"".equals(height))
            model.put("height", height);
        if (width != null && !"".equals(width))
            model.put("width", width);
        if (autoplay != null && !"".equals(autoplay))
            model.put("autoplay", autoplay);
        else
            model.put("autoplay", "false");

        Resource mediaResource = null;
        if (url != null && url.startsWith("/")) {
            try {
                String token = SecurityContext.getSecurityContext().getToken();
                mediaResource = repository.retrieve(token, Path.fromString(url), false);
            } catch (Exception e) {
            }
        }

        String extension = getExtension(url);
        if (contentType != null && !"".equals(contentType)) {
            model.put("contentType", contentType);
        } else if (mediaResource != null) {
            model.put("contentType", mediaResource.getContentType());
        } else if (extentionToMimetype.containsKey(extension)) {
            model.put("contentType", extentionToMimetype.get(extension));
        }

        model.put("extension", extension);
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URL, PARAMETER_URL_DESCRIPTION);
        map.put(PARAMETER_HEIGHT, PARAMETER_HEIGHT_DESCRIPTION);
        map.put(PARAMETER_WIDTH, PARAMETER_WIDTH_DESCRIPTION);
        map.put(PARAMETER_AUTOPLAY, PARAMETER_AUTOPLAY_DESCRIPTION);
        map.put(PARAMETER_CONTENT_TYPE, PARAMETER_CONTENT_TYPE_DESCRIPTION);
        return map;
    }
}
