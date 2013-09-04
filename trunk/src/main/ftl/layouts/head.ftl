<#ftl strip_whitespace=true>
<#--
  - File: head.ftl
  - 
  - Description: Manage head info; title, css and js
  - 
  -  Titles are applied in the following order: 
  -  ${htmlTitle} - ${title.title} - ${title.name} - [default title]
  -
  - Optional model data:
  -   resourceContext
  -   cssURLs
  -   jsURLs
  -   htmlTitle
  -   title
  -->
<#import "/lib/vortikal.ftl" as vrtx />
  
<#assign defaultTitle = ''/>
<#if resourceContext?exists && resourceContext.currentResource?exists>
  <#assign defaultTitle = vrtx.getMsg("title.title",
           resourceContext.currentResource.name,
           [resourceContext.currentResource.name])/>
</#if>
<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" type="text/css" />
  </#list>
</#if>
<#if jsURLs?exists>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>
<title>
<#if htmlTitle?exists>
  ${htmlTitle}
<#elseif (title.title)?exists>
  ${title.title}
<#elseif (title.name)?exists>
  ${title.name}
<#else>
  ${defaultTitle}
</#if>
</title>