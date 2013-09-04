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
<#assign authors = vrtx.propValue(resourceContext.currentResource, "authors", "enumerated") />
<#if authors != "">
  <span class="vrtx-authors"><span class="vrtx-authors-prefix"><@vrtx.msg code="article.by" /></span> ${authors?html}</span>
</#if>
