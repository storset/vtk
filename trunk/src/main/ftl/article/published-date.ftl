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
  <span class="vrtx-published-date"><@vrtx.msg code="article.published" />&nbsp;${published}</span>
</#if>
