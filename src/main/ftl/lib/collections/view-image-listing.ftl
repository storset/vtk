<#import "../vortikal.ftl" as vrtx />

<#macro displayImages imageListing collection>
  <#local listingType = vrtx.propValue(collection, 'display-type', '', 'imgl') />
  <#if listingType == 'gallery'>
    <@displayGallery imageListing collection />
  <#elseif listingType == 'table'>
    <@displayTable imageListing collection />
  <#else>
    <@displayDefault imageListing collection />
  </#if>
</#macro>

<#macro displayDefault imageListing collection>

  <#-- MOVE TO CONFIG, INCLUDE IN HEAD-TAG -->
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/jquery-1.3.2.min.js"></script>
  <script type="text/javascript">
    <!--
    $(document).ready(function() {
        $(".vrtx-image-container").each(function (i) {
      
          var height = $(this).find("img").height();
          var maxSize = parseInt($(this).css('height')); //can vary depending on thumbnail size on host
          
          if (height > maxSize) { 
            $(this).find("img").css('height', maxSize + 'px');
          }
        
        });
    });
    // -->
  </script>

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
    <p/>
  </#if>
</#macro>

<#macro displayGallery imageListing collection>

  <#-- MOVE TO CONFIG, INCLUDE IN HEAD-TAG -->
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/jquery-1.3.2.min.js"></script>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/galleria/jquery.galleria.pack.js"></script>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/galleria/galleria.js"></script>
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/jquery/galleria/galleria.css" />
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/jquery/galleria/galleria.override.css" />

  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-gallery">
      <p class="nav">
        <a id="vrtx-image-gallery-previous" href="#" onclick="$.galleria.prev(); return false;">${vrtx.getMsg("imageListing.previous")}</a>
        <a id="vrtx-image-gallery-next" href="#" onclick="$.galleria.next(); return false;">${vrtx.getMsg("imageListing.next")}</a>
      </p>
      <ul class="vrtx-gallery">
        <#list images as image>
          <#local title = vrtx.propValue(image, 'title')?html />
            <#if (image_index == 0) >
              <li class="active">
            <#else>
              <li>
            </#if>
            <a href="${image.URI?html}" title="${title}"><img src="${image.URI?html}?vrtx=thumbnail" alt="${title}"></a></li>
        </#list>
      </ul>
    </div>
 </#if>
</#macro>

<#macro displayTable imageListing collection>

  <#-- MOVE TO CONFIG, INCLUDE IN HEAD-TAG -->
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/tablesort.js"></script>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/customsort-norwegian-mod.js"></script>
  
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
            <#local owner = vrtx.propValue(image, 'owner', 'short', '') />
            <#local ownerUrl = vrtx.propValue(image, 'authorUrl', 'short', 'content') />
            <td>${owner} ${ownerUrl}</td>
            <#local creationTime = vrtx.propValue(image, 'creationTime', 'short', '') />
            <td>${creationTime}</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
  </#if>
</#macro>
