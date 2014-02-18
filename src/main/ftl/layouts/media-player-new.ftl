<#ftl strip_whitespace=true>

<#--
  - File: media-player-new.ftl
  - 
  - Description: Media player
  - 	
  -->

<#import "/lib/media-player.ftl" as mpLib />

<#macro mediaPlayer>

  <#assign dateStr = nanoTime?c />
  
  <#-- Minimum Flash Player version required: -->
  <#assign flashPlayerVersion = "10.2.0" />

  <p>Debug: Streaming video from Wowza</p>

  <@mpLib.includeFlash  />
  <@mpLib.genPlaceholder "${streamingUrls.hlsStreamUrl?html}" "${dateStr}" />
  
  <script type="text/javascript"><!--
    var flashvars = {
  	  src: "${directStreamingUrls.hdsStreamUrl?url("UTF-8")}",
  	  streamType: "live"
  	  <#if poster?exists>,poster: "${poster?html}" </#if>
  	  <#if autoplay?exists>,autoPlay: "${autoplay}"</#if>
    };
	var params = {																																														
	  allowFullScreen: "true",
	  allowscriptaccess: "always"
	};
	swfobject.embedSWF("${strobe?html}", "mediaspiller-${dateStr}", "${width}", "${height}", "${flashPlayerVersion}", false, flashvars, params);
  // -->
  </script>
</#macro>