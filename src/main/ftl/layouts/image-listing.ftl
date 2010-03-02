<#if !excludeScripts?exists>
<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
  </#list>
</#if>

<#if jsURLs?exists && type == 'simple-gallery'>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>
</#if>

<#if images?exists>
  <div class="vrtx-image-listing-include">
    <span class="vrtx-image-listing-include-title"><a href="${folderUrl}?display=gallery">${folderTitle}</a></span>
    <#if type == 'simple-gallery'>
      <#list images as image>
        <div class="vrtx-image-listing-include-container-pure-css">
          <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery">
            <img src="${image.URI}" alt="${image.URI}" />
          </a>
        </div>
        <#break />
      </#list>
    </#if>
    <ul>
    <#assign first = 'true' />
    <#list images as image>
      <#if first == 'true' && type == 'simple-gallery'>
        <li><a class="active" href="${folderUrl}?actimg=${image.URI}&amp;display=gallery"><img src="${image.URI}?vrtx=thumbnail" /></a></li>
        <#assign first = 'false' />
      <#else>
        <li><a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery"><img src="${image.URI}?vrtx=thumbnail" /></a></li>
      </#if>
    </#list>
    </ul>
  </div>
</#if>