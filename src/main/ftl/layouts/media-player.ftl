<#ftl strip_whitespace=true>

<#--
  - File: media-player.ftl
  - 
  - Description: Article media player
  - 
  - Required model data:
  -   resource
  -
  -->
  
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign mediaRes = vrtx.propResource(resourceContext.currentResource, "media", "resource") />
<#assign media = vrtx.propValue(resourceContext.currentResource, "media", "", "resource") />
 
<@viewutils.displayMediaPlayer mediaRes media />