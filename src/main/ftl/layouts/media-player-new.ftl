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
</#macro>