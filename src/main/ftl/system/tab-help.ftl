<#ftl strip_whitespace=true>
<#if tabHelpURL?exists>
  <a class="tabHelpURL" <#if tabHelpURL.target?exists> target="${tabHelpURL.target?html}"</#if>
     href="${tabHelpURL.url?html}">${tabHelpURL.description?html}</a>
</#if>
