<#ftl strip_whitespace=true>

<#--
  - File: media-refs.ftl
  - 
  - Description: Article media references
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign mediaRes = vrtx.propResource(resourceContext.currentResource, "media") />
<#assign media = vrtx.propValue(resourceContext.currentResource, "media") />
 
<@viewutils.displayMediaPlayer mediaRes media />
