<#ftl strip_whitespace=true>

<#--
  - File: introduction.ftl
  - 
  - Description: Article introduction text
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<#-- Introduction -->
<#assign introduction = vrtx.propValue(resourceContext.currentResource, "introduction") />
<#if introduction != "">
  <div class="vrtx-introduction">${introduction}</div>
</#if>
