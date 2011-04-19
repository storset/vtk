<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<#macro mediaPlayer >

<#if media?exists && contentType?exists >
<script type="text/javascript" src="/vrtx/__vrtx/static-resources/flash/StrobeMediaPlayback_1.0-full/10.1/scripts/swfobject.js"></script>
  <div class="vrtx-media-ref">
  
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3" || contentType == "audio/x-mpeg">
      <script type="text/javascript" src="${audioFlashPlayerJsURL?html}/"></script>  
    
      <object type="application/x-shockwave-flash" data="${audioFlashPlayerFlashURL?html}" height="24" width="290">
        <param name="movie" value="${audioFlashPlayerFlashURL?html}"/>
        <param name="FlashVars" value="playerID=1&amp;soundFile=${media?url("UTF-8")}<#if autoplay?exists && autoplay = "true">&amp;autostart=yes</#if>"/>
        <param name="quality" value="high"/>
        <param name="menu" value="false"/>
        <param name="wmode" value="transparent"/>
        <a class="vrtx-media" href="${media?html}"><img src="/vrtx/__vrtx/static-resources/themes/default/icons/audio-icon.png" width="151" height="82" alt="<@vrtx.msg code="article.audio-file" />"/></a> 
      </object>
      <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.audio-file" /></a>
    
     <#elseif contentType == "video/quicktime" >
    
      <object classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B" id="testid" width="${width}" height="${height}" codebase="http://www.apple.com/qtactivex/qtplugin.cab">
        <param name="src" value="${media?html}"/>
        <param name="autoplay" value="<#if autoplay?exists && autoplay = "true">true<#else>false</#if>"/>
        <param name="controller" value="true"/>
        <param name="loop" value="false"/>
        <param name="scale" value="aspect" />         
        <embed src="${media?html}" width="${width}" height="${height}" autoplay="<#if autoplay?exists && autoplay = "true">true<#else>false</#if>" controller="true" loop="false" scale="aspect" pluginspage="http://www.apple.com/quicktime/download/">
        </embed>
      </object> 
	  <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
	  <script type="text/javascript">
    swfobject.registerObject("testid", "9.0.115");
    </script>
	
	<#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
	
		<OBJECT 
			type="application/x-shockwave-flash"
			data="${media?url("UTF-8")}" 
			width="${width}" 
			height="${height}">
		    <PARAM name="movie" value="${media?url("UTF-8")}" />
		    <PARAM name="FlashVars" value="autoplay=${autoplay}" />
		</OBJECT>
		
	<#elseif contentType == "video/x-flv"  || contentType == "video/mp4">
		<div id="testalternativ">
		<a class="vrtx-media" href="${media?html}"><img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-icon.png" width="151" height="82" alt="<@vrtx.msg code="article.media-file" />"/></a>
		</div>
		<script type="text/javascript">
		var flashvars = {
  		src: "${media?url("UTF-8")}"
  		<#if autoplay?exists>,autoPlay: "${autoplay}"</#if>
		};
		var params = {
		allowFullScreen: "true",
		allowscriptaccess: "always"
		}		
		swfobject.embedSWF("${strobe?html}", "testalternativ", "${width}", "${height}", "9.0.0",false,flashvars,params);
		</script>
		<#if contentType == "video/mp4" && !media?starts_with("rtmp")>
			<a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.video-file" /></a>
		</#if>
    <#else>
      	<a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  </div>
  
</#if>
</#macro>

<@mediaPlayer />
