<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#assign dateStr = "" />
<#assign strobeVersion = "10.1.0" />
<#assign rootResources = "/vrtx/__vrtx/static-resources/themes/default/" />
<script type="text/javascript"><!--
  if (typeof swfobject == "undefined") {
    document.write("<scr" + "ipt src='/vrtx/__vrtx/static-resources/flash/StrobeMediaPlayback_1.5.1-patched/10.1/scripts/swfobject.js' type='text/javascript'><\/script>");
  }
// -->
</script>

<#macro mediaPlayer >
  <#if media?exists>
    <#if poster?exists> <#assign vidImgSrc = "${poster?html}" />
    <#else>             <#assign vidImgSrc = "${rootResources}icons/video-noflash.png" />
    </#if>              <#assign audioImgSrc = "${rootResources}icons/audio-icon.png" />
    
    <#if streamType?exists>
      <#assign dateStr = nanoTime?c />
      <@genPrintImage vidImgSrc />
      <@includeMediaPlayerMarkup vidImgSrc "article.media-file" "" "500" "279" />       
      <@initVideoJS true />
    <#elseif contentType?exists>
      <#assign dateStr = nanoTime?c />
      <#if contentType == "audio"
        || contentType == "audio/mpeg"
        || contentType == "audio/mp3"
        || contentType == "audio/x-mpeg">
        <@genPrintImage audioImgSrc />
        <@includeMediaPlayerMarkup audioImgSrc "article.audio-file" "" "151" "82" />                     
        <@initAudioJS />
        <@showDownloadLink "article.audio-file" />
      <#elseif contentType == "video/quicktime" >
        <@genPrintImage vidImgSrc />
        <@initVideoQuicktime />
        <@showDownloadLink "article.media-file" />
      <#elseif contentType == "application/x-shockwave-flash" && extension == "swf">
        <@genPrintImage vidImgSrc />
        <@includeMediaPlayerMarkup vidImgSrc "article.media-file" "" "500" "279" />
        <@initVideoJS false true />
      <#elseif contentType == "video/x-flv"
            || contentType == "video/mp4">
        <@genPrintImage vidImgSrc />
        <@includeMediaPlayerMarkup vidImgSrc "article.video-file" "vrtx-media-player-no-flash" "" "" true />
        <@initVideoJS />
        <#if contentType == "video/mp4" && !media?starts_with("rtmp")>
          <@showDownloadLink "article.video-file" />
        </#if>
      <#else>
        <#assign showDL = "true" />
        <@showDownloadLink "article.media-file"  />
      </#if>
    </#if>
  </#if>
</#macro>

<#macro includeMediaPlayerMarkup imgSrc alt class="" w="" h="" linkedImg=false>
  <div id="mediaspiller-${dateStr}"<#if class !=""> class="${class}"</#if>>
    <#if linkedImg>
      <a class="vrtx-media" href="${media?html}">
    </#if>
        <img src="${imgSrc}" alt="<@vrtx.msg code=alt />" <#if w != ""> width="${w}"</#if><#if h != ""> height="${w}"</#if> />
	<#if linkedImg>
	  </a>
	<#else>
	  <a class="playbutton" href="${media?html}"></a>
	  <@includeNoFlashCSS />
	</#if>
  </div>
</#macro>

<#macro initVideoJS isLiveStream=false isFlash=false>
  <script type="text/javascript"><!--
    <#if !isFlash>
      var flashvars = {
  	    src: "${media?url("UTF-8")}"
  	    <#if isLiveStream>,streamType: "live"</#if>
  	    <#if poster?exists>,poster: "${poster?url("UTF-8")}" 
  	    <#else>,poster: "${rootResources}icons/video-noflash.png"</#if>
  	    <#if autoplay?exists>,autoPlay: "${autoplay}"</#if>
	  };
	  var flashparams = {
	    allowFullScreen: "true",
	    allowscriptaccess: "always"
	  };
	<#else>
	  var flashvars = {
        <#if autoplay?exists>autoplay: "${autoplay}"</#if>
	  };
	  var flashparams = {};
      var flashattr = {};
	</#if>
	swfobject.embedSWF(<#if !isFlash>"${strobe?html}"<#else>"${media?html}"</#if>, "mediaspiller-${dateStr}", "${width}", "${height}", "${strobeVersion}", false, flashvars, flashparams<#if isFlash>, flashattr</#if>);
  // -->
  </script>
</#macro>

<#macro initVideoQuicktime>
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
</#macro>

<#macro initAudioJS>
  <script type="text/javascript"><!--
    var flashvars = {
	  playerID: "1",
  	  soundFile: "${media?url("UTF-8")}"
  	  <#if poster?exists>,poster: "${poster?html}" </#if>
  	  <#if autoplay?exists>,autoplay: "${autoplay}"</#if>
    };
    var params = {
      quality: "high",
	  menu: "false",
	  wmode: "transparent"
    };	
    swfobject.embedSWF("${audioFlashPlayerFlashURL?html}", "mediaspiller-${dateStr}", "290", "24", "${strobeVersion}", false, flashvars, params);
  // -->
  </script>
</#macro>

<#macro showDownloadLink linkText="article.media-file">
  <#if showDL?exists && showDL == "true">
    <a class="vrtx-media" href="${media?html}"><@vrtx.msg code=linkText /></a>
   </#if>
</#macro>

<#macro genPrintImage imgSrc>
  <img class="vrtx-media-print" src="${imgSrc}" alt="print video image" style="display: none" />
</#macro>

<#-- XXX: own file -->
<#macro includeNoFlashCSS>
<style type="text/css">
  .vrtx-media-player-no-flash,
  .vrtx-media-player-no-flash img {
    width: 507px;
    height: 282px;
    float: left;
  }
  .vrtx-media-player-no-flash {
    background-color: #000000;
    position: relative;
  }
  .vrtx-media-player-no-flash .playbutton { 
    position: absolute;
    /* take out of flow */
    top: 90px;
    left: 195px;
    width: 115px;
    height: 106px;
    display: block;
  }
  .vrtx-media-player-no-flash .playbutton,
  .vrtx-media-player-no-flash .playbutton:visited,
  .vrtx-media-player-no-flash .playbutton:active {
    background: url('${rootResources}icons/video-playbutton.png') no-repeat center center;
  }
  .vrtx-media-player-no-flash .playbutton:hover {
    background-image: url('${rootResources}icons/video-playbutton-hover.png');
  }
</style>
</#macro>

<@mediaPlayer />