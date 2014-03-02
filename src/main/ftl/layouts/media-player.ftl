<#ftl strip_whitespace=true>

<#--
  - File: layouts/media-player.ftl
  - 
  - Description: Media player macro
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/media-player.ftl" as mpLib />

<#macro mediaPlayer dateStr>
  <#if media?exists>
    <#if streamType?exists>
  
      <@mpLib.genPlaceholder "${media?html}" dateStr />
      <@mpLib.initFlash '${media?url("UTF-8")}' dateStr true />

    <#elseif contentType?exists>
    
      <#if contentType == "audio" 
        || contentType == "audio/mpeg"
        || contentType == "audio/mp3"
        || contentType == "audio/x-mpeg">

	    <@mpLib.genPlaceholder "${media?html}" dateStr true />
        <@mpLib.initFlash '${media?url("UTF-8")}' dateStr false true />

	    <@mpLib.genDownloadLink "${media?html}" "audio" />
	  
      <#elseif contentType == "video/quicktime" >
        <object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" id="testid" width="${width}" height="${height}" 
                codebase="http://www.apple.com/qtactivex/qtplugin.cab">
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
        
        <@mpLib.genDownloadLink "${media?html}" />
      
      <#elseif contentType == "application/x-shockwave-flash"
                           && extension == "swf">
    
	    <@mpLib.genPlaceholder "${media?html}" dateStr />
	    <@mpLib.initFlash '${media?url("UTF-8")}' dateStr false false true />

      <#elseif contentType == "video/x-flv"
            || contentType == "video/mp4">
    
	    <@mpLib.genPlaceholder "${media?html}" dateStr false true />
	    <@mpLib.initFlash '${media?url("UTF-8")}' dateStr />
	  
	    <#if contentType == "video/mp4" && !media?starts_with("rtmp")>
          <@mpLib.genDownloadLink "${media?html}" "video" />
	    </#if>
	  
      <#else>
    
        <@mpLib.genDownloadLink "${media?html}" "media" true />
      
      </#if>
    </#if>
  </#if>
</#macro>

<#-- When this layout template is invoked as "page" for components: -->
<#assign dateStr = 0 />
<#if nanoTime?has_content><#assign dateStr = nanoTime?c /></#if>
<@mediaPlayer dateStr />
