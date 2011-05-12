package org.vortikal.web.decorating.components;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final static String PARAMETER_STREAM_TYPE = "stream-type";
    private final static String PARAMETER_STREAM_TYPE_DESC = "Set to live for live stream";
    private final static String PARAMETER_POSTER = "poster";
    private final static String PARAMETER_POSTER_DESC = "Poster image for video";

    protected void processModel(Map<Object, Object> model, DecoratorRequest request, DecoratorResponse response)
            throws Exception {

        String url = request.getStringParameter(PARAMETER_URL);
        String height = request.getStringParameter(PARAMETER_HEIGHT);
        String width = request.getStringParameter(PARAMETER_WIDTH);
        String autoplay = request.getStringParameter(PARAMETER_AUTOPLAY);
        String contentType = request.getStringParameter(PARAMETER_CONTENT_TYPE);
        String streamType = request.getStringParameter(PARAMETER_STREAM_TYPE);
        String poster =  request.getStringParameter(PARAMETER_POSTER);

        mediaPlayer.addMediaPlayer(model, url, height, width, autoplay, contentType, streamType, poster);
    }

    protected Map<String, String> getParameterDescriptionsInternal() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PARAMETER_URL, PARAMETER_URL_DESCRIPTION);
        map.put(PARAMETER_HEIGHT, PARAMETER_HEIGHT_DESCRIPTION);
        map.put(PARAMETER_WIDTH, PARAMETER_WIDTH_DESCRIPTION);
        map.put(PARAMETER_AUTOPLAY, PARAMETER_AUTOPLAY_DESCRIPTION);
        map.put(PARAMETER_CONTENT_TYPE, PARAMETER_CONTENT_TYPE_DESCRIPTION);
        map.put(PARAMETER_STREAM_TYPE, PARAMETER_STREAM_TYPE_DESC);
        map.put(PARAMETER_POSTER, PARAMETER_POSTER_DESC);
        return map;
    }
}
