<#ftl strip_whitespace=true>
<#--
  - File: authors.ftl
  - 
  - Description: Article authors
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#assign published = vrtx.propValue(resourceContext.currentResource, "published-date") />
<#if published != "">
  ${published}
</#if>
