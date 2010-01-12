<#import "../vortikal.ftl" as vrtx />

<#macro displayImages imageListing collection>
  <#local galleryListing = vrtx.propValue(collection, 'display-type', '', 'imgl') == 'gallery' />
  <#if galleryListing == false>
    <@displayDefault imageListing collection />
  <#else>
    <@displayGallery imageListing collection />
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

  <#local images=imageListing.files />
  <#if (images?size > 0)>
    <div class="vrtx-image-gallery"> 
      <ul class="vrtx-gallery">
        <#list images as image>
          <#local title = vrtx.propValue(image, 'title')?html />
          
          <#local showAuthor = vrtx.propValue(collection, 'show-author', '', 'imgl') = 'true' />
              <#if showAuthor>
                <#local author = vrtx.propValue(image, 'authorName', '', 'content') />
                <#if author != "">
                  <#local title = title + " | " + author>
                </#if>
              </#if>
              
            <li><a href="${image.URI}" title="${title}"><img src="${image.URI}?vrtx=thumbnail" alt="${title}"></a></li>
        </#list>
      </ul>
      
      <#-- Move script in own file and localize-->
      <div id="vrtx-image-gallery-colors">
        View on:
        <a id="vrtx-display-on-white" href="#" onClick="$('body').css( { 'color': '#555', 'background-color': '#fff' } ); $('#vrtx-display-on-white:link').css('color', '#555'); 
        $('#vrtx-display-on-black:link').css('color', '#334488');">White</a> |
        <a id="vrtx-display-on-black" href="#" onClick="$('body').css( { 'color': '#eee', 'background-color': '#000' } ); $('#vrtx-display-on-black:link').css('color', '#eee'); 
        $('#vrtx-display-on-white:link').css( 'color', '#334488');">Black</a>
      </div>
    </div>
 </#if>
</#macro>