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
    <script type="text/javascript"><!--
       $(window).load(function() {
         if($("#right-main").length) {
           var cut = ".last-four";
         } else {
           var cut = ".last-five";
         }
	     $('ul.vrtx-image-listing').find(".vrtx-image-entry:not(" + cut + ")")
	       .css("marginRight", "18px !important;").end()
	       .masonry({singleMode: false});
	   });
     // -->
     </script>
    <div class="vrtx-image-listing-container">
      <ul class="vrtx-image-listing">
      <#assign count = 1 />
      <#list images as image>
        <#local title = vrtx.propValue(image, 'title')?html />
        <#if count % 4 == 0 && count % 5 == 0>
          <li class="vrtx-image-entry last last-four last-five">
        <#elseif count % 4 == 0>
          <li class="vrtx-image-entry last last-four">
        <#elseif count % 5 == 0>
          <li class="vrtx-image-entry last-five">
        <#else>
          <li class="vrtx-image-entry">
        </#if>
            <div class="vrtx-image-container">
                <a href="${imageListing.urls[image.URI]?html}"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" title="${title}" alt="${title}" /></a>
            </div>

            <div class="vrtx-image-info">
              <div class="vrtx-image-title">
                <#if (title?string?length > 20) >
                  <a href="${imageListing.urls[image.URI]?html}">${title?substring(0, 20)}...</a>
                <#else>
                  <a href="${imageListing.urls[image.URI]?html}">${title}</a>
                </#if>
              </div>
              
              <#local creationTime = vrtx.propValue(image, 'creationTime', 'short', '') />
              <div class="vrtx-image-creation-time">
                ${creationTime}
              </div>
              
              <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
              <div class="vrtx-image-description">
                <#if description?has_content>
                  <@vrtx.flattenHtml value=description escape=false />
                </#if>
              </div>
            </div>
        </li>
        <#assign count = count +1 />
      </#list>
      </ul>
    </div>
  </#if>

</#macro>

<#macro displayGallery imageListing collection>
  
  <#local images=imageListing.files />
  <#if (images?size > 0)>
   <div class="vrtx-image-listing-include" id="vrtx-image-folder-gallery">
      
      <#local activeImage = "" />
      <#if RequestParameters['actimg']?exists>
        <#local activeImage = RequestParameters['actimg'] />
      </#if>
      
      <script type="text/javascript">
       <!--
		 $(window).load(function () {
				
		   var wrapper = ".vrtx-image-listing-include";	
		   var container = ".vrtx-image-listing-include-container";
			  
		   var options = {
		     fadeInOutTime : 0
		   }
			  
		   $(wrapper + " ul li a").vrtxSGallery(wrapper, container, 635, options);
				  
	     });
       // -->
       </script>
       
        <div class="vrtx-image-listing-include-container-pure-css">
          <div class="vrtx-image-listing-include-container-nav-pure-css">
            <a class="prev" href="#" title="${vrtx.getMsg('imageListing.previous.prefix')}&nbsp;${vrtx.getMsg('imageListing.previous')}"><span class="prev-transparent-block"></span></a>
            <a class="next" href="#" title="${vrtx.getMsg('imageListing.next')}&nbsp;${vrtx.getMsg('imageListing.next.postfix')}"><span class="next-transparent-block"></span></a>
          </div>
        </div>
    <ul class="vrtx-image-listing-include-thumbs-pure-css">
    <#assign count = 1 />
    <#list images as image>
        <#assign description = vrtx.propValue(image, 'description', '', 'content')?html />
        <#assign title = vrtx.propValue(image, 'title')?html />
        <#if count % 5 == 0 && count % 6 == 0>
          <li class="vrtx-thumb-last vrtx-thumb-last-five vrtx-thumb-last-six">
        <#elseif count % 5 == 0>
          <li class="vrtx-thumb-last vrtx-thumb-last-five">
        <#elseif count % 6 == 0>
          <li class="vrtx-thumb-last-six">
        <#else>
          <li>
        </#if>
        <#if activeImage != "">
	      <#if (activeImage == image.URI) >
	          <a href="${imageListing.urls[image.URI]?html}" class="active"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	        <#else>
	          <a href="${imageListing.urls[image.URI]?html}"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	        </#if>
	      <#else>
	        <#if (image_index == 0) >
	          <a href="${imageListing.urls[image.URI]?html}" class="active"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	        <#else>
	          <a href="${imageListing.urls[image.URI]?html}"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	        </#if>
	      </#if>
	            <span><img src="${imageListing.urls[image.URI]?html}" alt="${description}" title="${title}" /></span>
	          </a>
        </li>
        <#assign count = count+1 />
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
            <th id="vrtx-table-image">${vrtx.getMsg("property.resourceType.image")}</th>
            <th id="vrtx-table-title" class="sortable-text">${vrtx.getMsg("property.title")}</th>
            <th id="vrtx-table-description" class="sortable-text">${vrtx.getMsg("property.content:description")}</th>
            <th id="vrtx-table-dimensions-width" class="sortable-numeric">${vrtx.getMsg("imageListing.width")}</th>
            <th id="vrtx-table-dimensions-height" class="sortable-numeric">${vrtx.getMsg("imageListing.height")}</th>
            <th id="vrtx-table-size"class="sortable-numeric">${vrtx.getMsg("property.contentLength")}</th>
            <th id="vrtx-table-photo" class="sortable-text">${vrtx.getMsg("article.photoprefix")}</th>
            <th id="vrtx-table-creation-time" class="sortable-sortEnglishLonghandDateFormat">${vrtx.getMsg("proptype.name.creationTime")}</th>
          </tr>
        </thead>
        <tbody>
        <#list images as image>
          <tr>
            <#local title = vrtx.propValue(image, 'title')?html />
            <td class="vrtx-table-image"><a href="${imageListing.urls[image.URI]?html}"><img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${title}" /></a></td>
            <td class="vrtx-table-title"><a href="${imageListing.urls[image.URI]?html}">${title}</a></td>
            <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
            <td class="vrtx-table-description">
              <#if description?has_content>
                <@vrtx.flattenHtml value=description escape=false />
              </#if>
            </td>
            <#local width = vrtx.propValue(image, 'pixelWidth') />
            <#local height = vrtx.propValue(image, 'pixelHeight') />
            <td class="vrtx-table-dimensions-width">${width} px</td>
            <td class="vrtx-table-dimensions-height">${height} px</td>
            <#local contentLength = vrtx.propValue(image, 'contentLength') />
            <td class="vrtx-table-size"><@vrtx.calculateResourceSizeToKB contentLength?number /></td>
            <#local photo = vrtx.propValue(image, 'authorName', '', 'content')> 
            <td class="vrtx-table-photo">${photo}</td>
            <#local creationTime = vrtx.propValue(image, 'creationTime', 'short', '') />
            <td class="vrtx-table-creation-time">${creationTime}</td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
  </#if>

</#macro>
