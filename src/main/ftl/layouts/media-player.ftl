<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Media player component
  - 	
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/media-player-new.ftl" as mpNew />
<#import "/layouts/media-player-old.ftl" as mpOld />

<#macro mediaPlayer>
  <#local dateStr = 0 />
  <#if nanoTime?has_content>
    <#local dateStr = nanoTime?c />
  </#if>
  <#if directStreamingUrls?exists>
    <@mpNew.mediaPlayer dateStr />
  <#else>
    <@mpOld.mediaPlayer dateStr />
  </#if>
</#macro>

<@mediaPlayer />