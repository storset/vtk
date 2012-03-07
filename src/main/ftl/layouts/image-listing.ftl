<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/gallery.ftl" as gallery />

<#if !excludeScripts?exists>
  <#if jsURLs?exists && type?exists && type == 'gallery'>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
</#if>

<#if images?exists>
  <#assign maxWidth = 507 />
  <#assign maxHeight = 380 />
  
  <div class="vrtx-image-listing-include">
    <span class="vrtx-image-listing-include-title"><a href="${folderUrl}?display=gallery">${folderTitle}</a></span>
    <#if type?exists && type = 'gallery'>
      <@gallery.galleryJSInit maxWidth fadeEffect />
      <ul class="vrtx-image-listing-include-thumbs-pure-css">
    <#else>
      <ul class="vrtx-image-listing-include-thumbs">
    </#if>
        <@gallery.galleryListImages images maxWidth maxHeight />
      </ul>
  </div>
</#if>
