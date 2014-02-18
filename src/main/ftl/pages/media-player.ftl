<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player.ftl" as mediaPlayer />

<#assign title = vrtx.propValue(resourceContext.currentResource, "title" , "flattened") />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <style type="text/css">
    a.vrtx-media {
      clear: left;
      display: block;
      margin: 10px 0px 0px;
    }
  </style>
</head>
<body>

<h1>${title}</h1>

<#if description?exists >
  <div id="vrtx-meta-description">
    ${description}
  </div>
</#if>

<@mediaPlayer.mediaPlayer />

<!-- From optional videoapp extension: -->
<#if streamingUrls?exists>
  <#if streamingUrls.hdsStreamUrl?exists || streamingUrls.hlsStreamUrl?exists>
  <h2>Streaming links</h2>
  <#if streamingUrls.hdsStreamUrl?exists>
    <p><a href="${streamingUrls.hdsStreamUrl?html}">Adobe Http Dynamic Streaming stream</a></p>
  </#if>
  <#if streamingUrls.hlsStreamUrl?exists>
    <p><a href="${streamingUrls.hlsStreamUrl?html}">Apple Http Live Streaming stream</a></p>
  </#if>
  <#else>
  <p>Streaming links not yet available. Try again in a few moments.</p>
  </#if>
</#if>

<!-- From optional videoapp extension: -->
<#if directStreamingUrls?exists>
  <p>HDS direct stream: ${directStreamingUrls.hdsStreamUrl}</p>
  <p>HLS direct stream: ${directStreamingUrls.hlsStreamUrl}</p>
</#if>

</body>
</html>
