<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 	
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player-new.ftl" as mpNew />
<#import "/layouts/media-player-old.ftl" as mpOld />

<#macro mediaPlayer>
  <#if directStreamingUrls?exists>
    <@mpNew.mediaPlayer />
  <#else>
    <@mpOld.mediaPlayer />
  </#if>
</#macro>

<@mediaPlayer />