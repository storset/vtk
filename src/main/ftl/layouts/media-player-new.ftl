<#ftl strip_whitespace=true>

<#--
  - File: media-player-new.ftl
  - 
  - Description: Media player new
  - 	
  -->

<#import "/lib/media-player.ftl" as mpLib />

<#macro mediaPlayer dateStr>

  <@mpLib.genPlaceholder "${streamingUrls.hlsStreamUrl?html}" dateStr />
  <@mpLib.initFlash '${directStreamingUrls.hdsStreamUrl?url("UTF-8")}' dateStr true  />
  <@mpLib.genDownloadLink "${media?html}" />

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

</#macro>