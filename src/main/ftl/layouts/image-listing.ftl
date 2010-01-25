
<#if images?exists>
<div class="vrtx-image-listing-include">
  <ul>
  <#list images as image>
  <li><img src="${image.URI}?vrtx=thumbnail" /></li>
  </#list>
  </ul>
</div>
</#if>