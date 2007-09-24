<#--
  - File: head-css.ftl
  - 
  - Description: Creates links to css stylesheets
  - 
  - Optional model data:
  -   cssURLs
  -->
<#if cssURLs?exists>
<#list cssURLs as cssURL>
  <link rel="stylesheet" href="${cssURL}">
</#list>
</#if>
