<#import "../vortikal.ftl" as vrtx />

<#macro displayImages imageListing collection>
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
             
              <#local author = vrtx.propValue(image, 'authorName', '', 'content') />
                <div class="vrtx-image-author">
                  ${author}
                </div>
              
            </div>
          
        </li>
      </#list>
      </ul>
    </div>
    <p/>
  </#if>
</#macro>