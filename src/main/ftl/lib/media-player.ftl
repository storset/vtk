<#ftl strip_whitespace=true>

<#--
  - File: lib/media-player.ftl
  - 
  - Description: Media player library
  -
  -->

<#-- Minimum Flash Player version required: -->
<#assign flashPlayerVersion = "10.2.0" />

<#--
 * genPlaceholder
 *
 * Generates a placeholder for a media file
 *
 * @param url the HTML-escaped URL to the media file
 * @param dateStr Unix-time in nanoseconds
 * @param isAudio (optional) is placeholder an audio file
 * @param showPlayButton (optional) do we want to show placeholder play button
 *
-->
<#macro genPlaceholder url dateStr isAudio=false showPlayButton=false>
  <#if !isAudio>
    <#local imgSrc = "video-noflash.png" />
    <#local width = "500" />
    <#local height = "279" />
    <#local alt = vrtx.getMsg("article.media-file") />
  <#else>
    <#local imgSrc = "audio-icon.png" />
    <#local width = "151" />
    <#local height = "82" />
    <#local alt = vrtx.getMsg("article.audio-file") />
  </#if>
  
  <@genPlayButtonCSS showPlayButton />
  
  <div id="mediaspiller-${dateStr}-print" class="vrtx-media-player-print<#if showPlayButton> vrtx-media-player-no-flash</#if>">
    <img src="<#if poster?exists>${poster?html}<#else>/vrtx/__vrtx/static-resources/themes/default/icons/${imgSrc}</#if>" width="${width}" height="${height}" alt="${alt}"/>
    <#if showPlayButton><a class="playbutton" href="${url}"></a></#if>
  </div>
  <div id="mediaspiller-${dateStr}"<#if showPlayButton> class="vrtx-media-player-no-flash"</#if>>
    <a class="vrtx-media" href="${url}">
	  <img src="<#if poster?exists>${poster?html}<#else>/vrtx/__vrtx/static-resources/themes/default/icons/${imgSrc}</#if>" width="${width}" height="${height}" alt="${alt}"/>
      <#if showPlayButton><a class="playbutton" href="${url}"></a></#if>
    </a>
  </div>
</#macro>

<#--
 * genPlayButtonCSS
 *
 * Generates CSS for play button
 *
 * @param showPlayButton (optional) do we want to show placeholder play button
 *
-->
<#macro genPlayButtonCSS showPlayButton>
  <style type="text/css">
    .vrtx-media-player-print { display: none; }
    <#if !showPlayButton>
    .vrtx-media-player-no-flash, .vrtx-media-player-no-flash img { width: 507px; height: 282px; float: left; }
    .vrtx-media-player-no-flash { background-color: #000000; position: relative; }
    .vrtx-media-player-no-flash .playbutton { 
      position: absolute; top: 90px; left: 195px; width: 115px; height: 106px; display: block;
    }
    .vrtx-media-player-no-flash .playbutton,.vrtx-media-player-no-flash .playbutton:visited,.vrtx-media-player-no-flash .playbutton:active {
      background: url('/vrtx/__vrtx/static-resources/themes/default/icons/video-playbutton.png') no-repeat center center;
    }
    .vrtx-media-player-no-flash .playbutton:hover { background-image: url('/vrtx/__vrtx/static-resources/themes/default/icons/video-playbutton-hover.png'); }
    </#if>
  </style>
</#macro>

<#--
 * initFlash
 *
 * Initialize Flash with JavaScript
 *
 * @param url the URL-escaped (UTF-8) URL to the media file
 * @param dateStr Unix-time in nanoseconds
 * @param isStream (optional) is a live stream
 * @param isAudio (optional) is an audio file
 * @param isSWF (optional) is a Shockwave-SWF file
 *
-->
<#macro initFlash url dateStr isStream=false isAudio=false isSWF=false>
  <#local flashUrl = strobe?html />
  
  <script type="text/javascript"><!--
    if (typeof swfobject == 'undefined') {
      document.write("<scr" + "ipt src='/vrtx/__vrtx/static-resources/flash/SMP_2.0.2494-patched/10.2/lib/swfobject.js' type='text/javascript'><\/script>");
    }
  // -->
  </script>
  <script type="text/javascript"><!--
    var flashvars = {
      <#if autoplay?exists>
        autoplay: "${autoplay}"
      </#if>
    };
    var flashparams = {};
    
    <#-- Video -->
    <#if !isAudio>
	  <#if !isSWF>
        flashvars.src = "${url}";
        <#if isStream>
          flashvars.streamType = "live";
        </#if>
        <#if poster?exists>
          flashvars.poster = "${poster?url("UTF-8")}";
        <#else>
          flashvars.poster = "/vrtx/__vrtx/static-resources/themes/default/icons/video-noflash.png";
        </#if>
	    flashparams = {																																														
	      allowFullScreen: "true",
	      allowscriptaccess: "always"
	    };
	    
	  <#-- SWF -->
	  <#else>
	    <#local flashUrl = url />
	  </#if>
	  
	<#-- Audio -->
    <#else>
	  flashvars.playerID = "1";
  	  flashvars.soundFile = "${url}";
	  flashparams = {
		quality: "high",
		menu: "false",
	    wmode: "transparent"
	  };	
	  <#local flashUrl = audioFlashPlayerFlashURL?html />
	  <#local width = "290" />
	  <#local height = "24" />
    </#if>
    
    swfobject.embedSWF("${flashUrl}", "mediaspiller-${dateStr}", "${width}", "${height}", "${flashPlayerVersion}", false, flashvars, flashparams);
  // -->
  </script>
</#macro>

<#--
 * genDownloadLink
 *
 * Generate download link to media file
 *
 * @param url the HTML-escaped URL to the media file
 * @param type (optional) can specify more narrow file type: "audio" or "video"
 * @param bypass (optional) can bypass check for showDL-boolean
 *
-->
<#macro genDownloadLink url type="media" bypass=false>
  <#if bypass || (showDL?exists && showDL == "true")>
    <a class="vrtx-media" href="${url?html}">
      <#if type = "video">
        <@vrtx.msg code="article.video-file" />
      <#elseif type = "audio">
        <@vrtx.msg code="article.audio-file" />
      <#else>
        <@vrtx.msg code="article.media-file" />
      </#if>
    </a>
  </#if>
</#macro>
