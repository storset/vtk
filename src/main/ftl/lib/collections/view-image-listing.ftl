<#import "../vortikal.ftl" as vrtx />

<#macro addScripts collection>

  <#local listingType = vrtx.propValue(collection, 'display-type', '', 'imgl') />
  <#if displayTypeParam?exists>
    <#local listingType = displayTypeParam />
  </#if>
  
  <#--
    Default listing chosen, no particular listing type given.
    Set it to "list" to retrieve proper scripts
  -->
  <#if !listingType?has_content>
    <#local listingType = "list" />
  </#if>
  
  <#if cssURLs?exists>
    <@addScriptURLs "css" "common" cssURLs />
    <@addScriptURLs "css" listingType cssURLs />
  </#if>
  <#if jsURLs?exists>
    <@addScriptURLs "js" "common" jsURLs />
    <@addScriptURLs "js" listingType jsURLs />
  </#if>
  
</#macro>

<#macro addScriptURLs scriptType listingType urls>
  
  <#if urls[listingType]?exists>
    <#list urls[listingType] as commonUrl>
      <#if scriptType == "css">
        <link rel="stylesheet" href="${commonUrl}" />
      <#elseif scriptType == "js">
        <script type="text/javascript" src="${commonUrl}"></script>
      </#if>
    </#list>
  </#if>

</#macro>

<#macro displayImages imageListing collection>

  <#local listingType = vrtx.propValue(collection, 'display-type', '', 'imgl') />
  <#if displayTypeParam?exists>
    <#local listingType = displayTypeParam />
  </#if>
  
  <#if listingType == 'gallery'>
    <@displayGallery imageListing collection />
  <#elseif listingType == 'table'>
    <@displayTable imageListing collection />
  <#else>
    <@displayDefault imageListing collection />
  </#if>

</#macro>

<#macro displayDefault imageListing collection>

  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-listing-container">
      <ul class="vrtx-image-listing">
      <#list images as image>
        <#local title = vrtx.propValue(image, 'title')?html />
        <li class="vrtx-image-entry">
        
            <div class="vrtx-image-container">
                <a href="${image.URI?html}"><img src="${image.URI?html}?vrtx=thumbnail" title="${title}" alt="${title}"></a>
            </div>

            <div class="vrtx-image-info">
              <div class="vrtx-image-title">
                <#if (title?string?length > 20) >
                  <a href="${image.URI?html}">${title?substring(0, 20)}...</a>
                <#else>
                  <a href="${image.URI?html}">${title}</a>
                </#if>
              </div>
              
              <#local creationTime = vrtx.propValue(image, 'creationTime', 'short', '') />
              <div class="vrtx-image-creation-time">
                ${creationTime}
              </div>
              
              <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
              <div class="vrtx-image-description">
                <#if description?has_content>
                  <#if (description?string?length > 20) >
                    ${description?substring(0, 20)}...
                  <#else>
                    ${description}
                  </#if>
                <#else>
                   
                </#if>
              </div>
            </div>
        </li>
      </#list>
      </ul>
    </div>
  </#if>

</#macro>

<#macro displayGallery imageListing collection>
  
  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-gallery">
      
      <a id="vrtx-image-gallery-previous" href="#" onclick="$.galleria.prev(); return false;"></a>
      <a id="vrtx-image-gallery-next" href="#" onclick="$.galleria.next(); return false;"></a>
      
      <#local activeImage = "" />
      <#if RequestParameters['actimg']?exists>
        <#local activeImage = RequestParameters['actimg'] />
      </#if>
      
      <ul class="vrtx-gallery">
        <#list images as image>
          <#local title = vrtx.propValue(image, 'title')?html />
            <#if activeImage != "">
              <#if (activeImage == image.URI) >
                <li class="active">
              <#else>
                <li>
              </#if>
            <#else>
              <#if (image_index == 0) >
                <li class="active">
              <#else>
                <li>
              </#if>
            </#if>
            <a href="${image.URI?html}" title="${title}"><img src="${image.URI?html}?vrtx=thumbnail" alt="${title}"></a></li>
        </#list>
      </ul>
    </div>
 </#if>

</#macro>

<#macro displayTable imageListing collection>

  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-table">
      <table class="rowstyle-alt colstyle-alt no-arrow" cellpadding="5" border="1">
        <thead>
          <tr>
            <th>${vrtx.getMsg("property.resourceType.image")}</th>
            <th class="sortable-text">${vrtx.getMsg("property.title")}</th>
            <th class="sortable-text">${vrtx.getMsg("property.content:description")}</th>
            <th class="sortable-numeric">${vrtx.getMsg("imageListing.width")}</th>
            <th class="sortable-numeric">${vrtx.getMsg("imageListing.height")}</th>
            <th class="sortable-numeric">${vrtx.getMsg("property.contentLength")}</th>
            <th class="sortable-text">${vrtx.getMsg("property.owner")}</th>
            <th class="sortable-sortEnglishLonghandDateFormat">${vrtx.getMsg("proptype.name.creationTime")}</th>
          </tr>
        </thead>
        <tbody>
        <#list images as image>
          <tr>
            <td class="vrtx-table-image"><a href="${image.URI}"><img src="${image.URI}?vrtx=thumbnail"/></a></td>
            <#local title = vrtx.propValue(image, 'title')?html />
            <td><a href="${image.URI}">${title}</a></td>
            <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
            <td>${description}</td>
            <#local width = vrtx.propValue(image, 'pixelWidth') />
            <#local height = vrtx.propValue(image, 'pixelHeight') />
            <td>${width} px</td>
            <td>${height} px</td>
            <#local contentLength = vrtx.propValue(image, 'contentLength') />
            <td><@vrtx.calculateResourceSizeToKB contentLength?number /></td>
            <#local owner = vrtx.propValue(image, 'owner') />
            <#if owner?index_of("@") != -1>
              <td>${owner}</td>
            <#else>
              <td><a href="http://www.uio.no/sok?person=${owner}">${owner}</a></td>
            </#if>
            <#local creationTime = vrtx.propValue(image, 'creationTime', 'short', '') />
            <td>${creationTime}</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
  </#if>

</#macro>
