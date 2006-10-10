<#--
  - File: portlet-page.ftl
  - 
  - Description: simple display of portlets on a page
  - 
  - Optional model data:
  -   createErrorMessage
  -   message
  -
  -->
<#import "/lib/portlet.ftl" as prtlt />
<#assign htmlTitle="Portal" />

<html>
<head>
  <#include "/layouts/head.ftl"/>
</head>

<body>

<#if !portlets?exists>
    No portlets to display
<#else>

  <#list portlets.portletList as portlet>
    <@prtlt.displayPortlet portlet=portlet />
  </#list>
</#if>
</body>
