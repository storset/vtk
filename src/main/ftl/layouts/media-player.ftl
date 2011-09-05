<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />

<#macro mediaPlayer >

    <#assign dateStr = nanoTime?c />
    <#assign strobeVersion = "10.1.0" />
  
    <script type="text/javascript"><!--
      if (typeof swfobject == 'undefined') {
        document.write("<scr" + "ipt src='/vrtx/__vrtx/static-resources/flash/StrobeMediaPlayback_1.5.1-patched/10.1/scripts/swfobject.js' type='text/javascript'><\/script>");
      }
    // -->
    </script> 
   
	  <div id="mediaspiller-${dateStr}" class="vrtx-media-player-no-flash" style="background-image: url('<#if poster?exists>${poster?html}<#else>/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png</#if>')">
	  
	  <link type="text/css" rel="stylesheet" media="all" href="/vrtx/__vrtx/static-resources/themes/default/view-mediaplayer.css" />

	  <a class="vrtx-media" href="${media?html}">
	  <div class="playbutton" style="float:left;margin-top:90px;width:115px;height:106px;margin-left:195px;">
	  
	  <!-- img src="/vrtx/__vrtx/static-resources/themes/default/icons/video-playbutton.png" border="0" / -->
	  
	  </div>
	  </a>
	  </div>
	  <script type="text/javascript"><!--
	    var flashvars = {
  		  src: "${media?url("UTF-8")}"
  		  <#if poster?exists>,poster: "${poster?url("UTF-8")}" 
  		  <#else>,poster: "/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png"
  		  </#if>
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
   
  

</#macro>

<@mediaPlayer />