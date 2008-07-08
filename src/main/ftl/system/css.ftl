<#if cssURLs?exists>
<#list cssURLs as cssURL>
  <link rel="stylesheet" href="${cssURL?html}" />
</#list>
</#if>
<#if serviceCssURLs?exists>
<#list serviceCssURLs as cssURL>
  <link rel="stylesheet" href="${cssURL?html}" />
</#list>
</#if>
