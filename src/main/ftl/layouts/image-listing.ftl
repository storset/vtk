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
          <a class="vrtx-image-listing-include-container-link" href="${folderUrl}?actimg=${image.URI}&amp;display=gallery">
            <img src="${image.URI}" alt="${image.URI}" />
          </a>
        </div>
        <div class="vrtx-image-listing-include-container-nav-pure-css">
          <a class='prev' href='#'>&lt;&lt; Forrige</a>
          <a class='next' href='#'>Neste &gt;&gt;</a>
        </div>
        <#break />
      </#list>
    </#if>
    <ul>
    <#list images as image>
        <li><a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery"><img src="${image.URI}?vrtx=thumbnail" /></a></li>
    </#list>
    </ul>
  </div>
</#if>