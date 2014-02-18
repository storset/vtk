<#ftl strip_whitespace=true>

<#--
  - File: media-player-old.ftl
  - 
  - Description: Media player old
  - 	
  -->

<#import "/lib/media-player.ftl" as mpLib />
 
<#macro mediaPlayer>

  <#if media?exists && streamType?exists>
    <@mpLib.genPlaceholder "${media?html}" />
    <@mpLib.initFlash '${media?url("UTF-8")}' true />

  <#elseif media?exists && contentType?exists>
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3" || contentType == "audio/x-mpeg">
      <#-- <script type="text/javascript" src="${audioFlashPlayerJsURL?html}/"></script> -->
      
	  <@mpLib.genPlaceholder "${media?html}" true />
      <@mpLib.initFlash '${media?url("UTF-8")}' false true />

	  <#if showDL?exists && showDL == "true">
        <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.audio-file" /></a>
      </#if>
    <#elseif contentType == "video/quicktime" >
      <object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" id="testid" width="${width}" height="${height}" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media?html}"/>
        <param name="autoplay" value="<#if autoplay?exists && autoplay = "true">true<#else>false</#if>"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />
        <embed src="${media?html}" 
               width="${width}" 
               height="${height}"
               autoplay="<#if autoplay?exists && autoplay = "true">true<#else>false</#if>"
               controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object>

      <#if showDL?exists && showDL == "true">
        <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
      </#if>
    <#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
	  <@mpLib.genPlaceholder "${media?html}" />
	  <@mpLib.initFlash '${media?url("UTF-8")}' false false true />

    <#elseif contentType == "video/x-flv" || contentType == "video/mp4">
	  <@mpLib.genPlaceholder "${media?html}" false true />
	  <@mpLib.initFlash '${media?url("UTF-8")}' />
	  
	  <#if contentType == "video/mp4" && !media?starts_with("rtmp")>
        <#if showDL?exists && showDL == "true">
          <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.video-file" /></a>
        </#if>
	  </#if>
    <#else>
      <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </#if>
</#macro>