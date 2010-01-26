
<#if !excludeScripts?exists>
<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
  </#list>
</#if>

<#if jsURLs?exists>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>
</#if>

<#if images?exists>
<div class="vrtx-image-listing-include">
  <span class="vrtx-image-listing-include-title"><a href="${folderUrl}">${folderTitle}</a></span>
  <ul>
  <#list images as image>
    <li><a href="${folderUrl}?actimg=${image.URI}"><img src="${image.URI}?vrtx=thumbnail" /></a></li>
  </#list>
  </ul>
</div>
</#if>