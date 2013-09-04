<#ftl strip_whitespace=true>
<#--
  - File: title.ftl
  - 
  - Description: Article title view
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#if flatten?exists && flatten == 'true'>
  ${vrtx.propValue(resourceContext.currentResource, "userTitle" , "flattened")}
<#else>
  <h1>${vrtx.propValue(resourceContext.currentResource, "userTitle")}</h1>
</#if>
