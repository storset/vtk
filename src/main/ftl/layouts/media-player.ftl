<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<#if media?exists && contentType?exists >

  <div class="vrtx-media-ref">
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3" >
    
      <script type="text/javascript" language="JavaScript" src="${audioFlashPlayerJsURL?html}/"></script>  
      <object type="application/x-shockwave-flash" data="${audioFlashPlayerFlashURL?html}" id="audioplayer1" height="24" width="290">
        <param name="movie" value="${audioFlashPlayerFlashURL?html}"/>
        <param name="FlashVars" value="playerID=1&amp;soundFile=${media}"/>
        <param name="quality" value="high"/>
        <param name="menu" value="false"/>
        <param name="wmode" value="transparent"/>
      </object>
      <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.audio-file" /></a>
    
     <#elseif contentType == "video/quicktime" >
    
      <object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" width="${width}" height="${height}" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media}"/>
        <param name="autoplay" value="false"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />         
        <embed src="${media}" width="${width}" height="${height}" autoplay="false" controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object> 
	  <a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>

	<#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
	
		<OBJECT 
			type="application/x-shockwave-flash"
			data="${media}" 
			width="${width}" 
			height="${height}">
		    <PARAM name="movie" value="${media}" />
		    <PARAM name="FlashVars" value="autoplay=${autoplay}" />
		</OBJECT>
		
	<#elseif (contentType == "application/x-shockwave-flash" && extension == "flv") || contentType == "video/mp4">

      	<object width="${width}" height="${height}"> 
			<param name="movie" value="${strobe?html}"></param>
			<param name="flashvars" value="src=${media}"></param>
			<param name="allowFullScreen" value="true"></param>
			<param name="allowscriptaccess" value="always"></param>	
			<embed src="${strobe?html}"
				type="application/x-shockwave-flash" 
			 	allowscriptaccess="always" 
			 	allowfullscreen="true" 
			 	width="${width}" 
			 	height="${height}" 
			 	flashvars="src=${media}">
		 	</embed>
		</object>
		<#if contentType == "video/mp4" && !media?starts_with("rtmp")>
			<a class="vrtx-media" href="${media}"><@vrtx.msg code="article.video-file" /></a>
		</#if>
    <#else>
      	<a class="vrtx-media" href="${media}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </div>
  
</#if>