<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<#macro mediaPlayer >
<#if media?exists && streamType?exists>
<#assign dateStr = nanoTime?c />
    <#assign strobeVersion = "10.1.0" />
	  <div id="mediaspiller-${dateStr}">
	    <a class="vrtx-media" href="${media?html}">
	      <img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png" width="500" height="279" alt="<@vrtx.msg code="article.media-file" />"/>
	    </a>
	  </div>
	  <script type="text/javascript"><!--
	    var flashvars = {
  		  src: "${media?url("UTF-8")}",
  		  streamType: "live"
  		  <#if autoplay?exists>,autoPlay: "${autoplay}"</#if>
	    };
	    var params = {
		  allowFullScreen: "true",
		  allowscriptaccess: "always"
	    };
	    swfobject.embedSWF("${strobe?html}", "mediaspiller-${dateStr}", "${width}", "${height}", "${strobeVersion}", false, flashvars, params);
	  // -->
	  </script>
	  
<#elseif media?exists && contentType?exists>
  
    <#assign dateStr = nanoTime?c />
    <#assign strobeVersion = "10.1.0" />
  
    <script type="text/javascript"><!--
      if (typeof swfobject == 'undefined') {
        document.write("<scr" + "ipt src='/vrtx/__vrtx/static-resources/flash/StrobeMediaPlayback_1.0-full/10.1/scripts/swfobject.js' type='text/javascript'><\/script>");
      }
    // -->
    </script> 
   
    <#if contentType == "audio" || contentType == "audio/mpeg" || contentType == "audio/mp3" || contentType == "audio/x-mpeg">
      <#-- <script type="text/javascript" src="${audioFlashPlayerJsURL?html}/"></script> -->
   	  <div id="mediaspiller-${dateStr}">
        <a class="vrtx-media" href="${media?html}">
          <img src="/vrtx/__vrtx/static-resources/themes/default/icons/audio-icon.png" width="151" height="82" alt="<@vrtx.msg code="article.audio-file" />"/>
        </a>
	  </div>
	  <script type="text/javascript"><!--
	    var flashvars = {
		  playerID: "1",
  		  soundFile: "${media?url("UTF-8")}"
  		  <#if autoplay?exists>,autoplay: "${autoplay}"</#if>
	    };
	    var params = {
		  quality: "high",
		  menu: "false",
		  wmode: "transparent"
	    };	
	    swfobject.embedSWF("${audioFlashPlayerFlashURL?html}", "mediaspiller-${dateStr}", "290", "24", "${strobeVersion}",false,flashvars,params);
	  // -->
	  </script>
      <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.audio-file" /></a>
    
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
      <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
	
    <#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
	  <div id="mediaspiller-${dateStr}">
	    <a class="vrtx-media" href="${media?html}">
	      <img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png" width="500" height="279" alt="<@vrtx.msg code="article.media-file" />"/>
	    </a>
	  </div>
	  <script type="text/javascript"><!--
		var flashvars = {
  		  <#if autoplay?exists>autoplay: "${autoplay}"</#if>
		};
		var flashparams = {};
		var flashattr = {};
		swfobject.embedSWF("${media?html}", "mediaspiller-${dateStr}", "${width}", "${height}", "${strobeVersion}", false, flashvars, flashparams, flashattr);
	  // -->
	  </script>
	  
    <#elseif (!streamType?exists) && contentType == "video/x-flv" || contentType == "video/mp4">
	  <div id="mediaspiller-${dateStr}">
	    <a class="vrtx-media" href="${media?html}">
	      <img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png" width="500" height="279" alt="<@vrtx.msg code="article.media-file" />"/>
	    </a>
	  </div>
	  <script type="text/javascript"><!--
	    var flashvars = {
  		  src: "${media?url("UTF-8")}"
  		  <#if autoplay?exists>,autoPlay: "${autoplay}"</#if>
	    };
	    var params = {
		  allowFullScreen: "true",
		  allowscriptaccess: "always"
	    };
	    swfobject.embedSWF("${strobe?html}", "mediaspiller-${dateStr}", "${width}", "${height}", "${strobeVersion}", false, flashvars, params);
	  // -->
	  </script>
	  <#if contentType == "video/mp4" && !media?starts_with("rtmp")>
	    <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.video-file" /></a>
	  </#if>
    <#else>
      <a class="vrtx-media" href="${media?html}"><@vrtx.msg code="article.media-file" /></a>
    </#if>
  
  </#if>
</#macro>

<@mediaPlayer />
