<link type="text/css" rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/image-listing-component.css" />

<#if images?exists>
<div class="vrtx-image-listing-include">
  <span class="vrtx-image-listing-include-title">${folderTitle}</span>
  <ul>
  <#list images as image>
    <li><a href="${folderUrl}?actimg=${image.URI}"><img src="${image.URI}?vrtx=thumbnail" /></a></li>
  </#list>
  </ul>
</div>
</#if>