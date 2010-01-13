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
  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-listing-container">
      <ul class="vrtx-image-listing">
      <#list images as image>
        <#local title = vrtx.propValue(image, 'title')?html />
        <li class="vrtx-image-entry">
        
            <div class="vrtx-image-container">
              <div class="vrtx-image">
                <a href="${image.URI}"><img src="${image.URI}?vrtx=thumbnail" title="${title}" alt="${title}"></a>
              </div>
            </div>

            <div class="vrtx-image-info">
              <div class="vrtx-image-title">
                <a href="${image.URI}">${title}</a>
              </div>
              
              <#local showDescription = vrtx.propValue(collection, 'show-description', '', 'imgl') = 'true' />
              <#if showDescription>
                <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
                <#if description?has_content>
                  <div class="vrtx-image-description">
                    ${description}
                  </div>
                </#if>
              </#if>
              
              <#local showKeywords = vrtx.propValue(collection, 'show-keywords', '', 'imgl') = 'true' />
              <#if showKeywords>
                <#local keywords = vrtx.propValue(image, 'keywords', '', 'content')?html />
                <#if keywords?has_content>
                  <div class="vrtx-image-keywords">
                    ${vrtx.getMsg("property.content:keywords")}: ${keywords}
                  </div>
                </#if>
              </#if>
              
              <#local showDimension = vrtx.propValue(collection, 'show-dimension', '', 'imgl') = 'true' />
              <#if showDimension>
                <#local width = vrtx.propValue(image, 'pixelWidth') />
                <#local height = vrtx.propValue(image, 'pixelHeight') />
                <div class="vrtx-image-dimension">
                  ${width} x ${height}
                </div>
              </#if>
             
              <#local showAuthor = vrtx.propValue(collection, 'show-author', '', 'imgl') = 'true' />
              <#if showAuthor>
                <#local author = vrtx.propValue(image, 'authorName', '', 'content') />
                <div class="vrtx-image-author">
                  ${author}
                </div>
              </#if>
              
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
  <script type="text/javascript">
      $(document).ready(function() {
        var htmlBGChange = "<div id='vrtx-image-gallery-colors'>${vrtx.getMsg("imageListing.view.on")}: ";
        htmlBGChange += "<a id='vrtx-display-on-white' href='#' onClick='toWhiteBG();'>${vrtx.getMsg("imageListing.view.on.white")}</a>";
        htmlBGChange += " | <a id='vrtx-display-on-black' href='#' onClick='toBlackBG();'>${vrtx.getMsg("imageListing.view.on.black")}</a>";
        htmlBGChange += "</div>";
        $(htmlBGChange).insertAfter("ul.vrtx-gallery");
      });
  </script>

  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-gallery">
      <p class="nav"><a href="#" onclick="$.galleria.prev(); return false;">previous</a> | <a href="#" onclick="$.galleria.next(); return false;">next</a></p>
      <ul class="vrtx-gallery">
        <#list images as image>
          <#local title = vrtx.propValue(image, 'title')?html />              
            <li><a href="${image.URI}" title="${title}"><img src="${image.URI}?vrtx=thumbnail" alt="${title}"></a></li>
        </#list>
      </ul>
    </div>
 </#if>
</#macro>

<#macro displayTable imageListing collection>
  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-table"> 
      <table>
        <thead>
          <tr>
            <th>${vrtx.getMsg("property.resourceType.image")}</th>
            <th>${vrtx.getMsg("property.title")}</th>
          </tr>
        </thead>
        <tbody>
        <#list images as image>
          <tr>
            <td><a href="${image.URI}"><img src="${image.URI}?vrtx=thumbnail"/></a></td>
            <#local title = vrtx.propValue(image, 'title')?html />
            <td>${title}</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
  </#if>
</#macro>      