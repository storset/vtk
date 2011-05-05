<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#--
  - File: gallery.ftl
  - 
  - Description: for use in gallery component and gallery folder
  -
  -->

<#macro galleryJSInit maxWidth fadeEffect>
  <script type="text/javascript"><!--
    $(window).load(function () {		
	  var wrapper = ".vrtx-image-listing-include";	
	  var container = ".vrtx-image-listing-include-container";	  
	  var options = {
	    fadeInOutTime : ${fadeEffect}
	  }
	  $(wrapper + " ul li a").vrtxSGallery(wrapper, container, ${maxWidth}, options);			  
    });
  // -->
  </script>
  <div class="vrtx-image-listing-include-container-pure-css">
    <div class="vrtx-image-listing-include-container-nav-pure-css">
      <a class="prev" href="#" title="${vrtx.getMsg('imageListing.previous.prefix')}&nbsp;${vrtx.getMsg('imageListing.previous')}"><span class="prev-transparent-block"></span></a>
      <a class="next" href="#" title="${vrtx.getMsg('imageListing.next')}&nbsp;${vrtx.getMsg('imageListing.next.postfix')}"><span class="next-transparent-block"></span></a>
    </div>
  </div>
</#macro>

<#macro galleryListImages images maxWidth maxHeight activeImage="" imageListing="">
  <#local count = 1 />
  <#list images as image>
    <#local description = vrtx.propValue(image, 'description', '', 'content')?html />
    <#local title = vrtx.propValue(image, 'title')?html />
    <#local width = vrtx.propValue(image, 'pixelWidth') />
    <#local height = vrtx.propValue(image, 'pixelHeight') />
    
    <#if height != "" && width != "">
      <#local width = width?number />
      <#local height = height?number />

      <#local percentage = 1 />
      <#if (width > height)>
        <#if (width > maxWidth)>
          <#local percentage = (maxWidth / width) />
        </#if>
      <#else>
        <#if (height > maxHeight)>
          <#local percentage = (maxHeight / height) />
        </#if>
      </#if>
      <#local width = (width * percentage)?round />
      <#local height = (height * percentage)?round />
      <#if (height > maxHeight)>
        <#local percentage = (maxHeight / height) />
        <#local width = (width * percentage)?round />
        <#local height = (height * percentage)?round />
      </#if>
    <#else>
      <#local width = 0 />
      <#local height = 0 />
    </#if>

    <#if count % 4 == 0 && count % 5 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-four vrtx-thumb-last-five">
    <#elseif count % 5 == 0 && count % 6 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-five vrtx-thumb-last-six">
    <#elseif count % 4 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-four">
    <#elseif count % 5 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-five">
    <#elseif count % 6 == 0>
      <li class="vrtx-thumb-last-six">
    <#else>
      <li>
    </#if>
    
    <#if activeImage != "" && imageListing != "">
	  <#if (activeImage == image.URI) >
	     <a href="${imageListing.urls[image.URI]?html}" class="active">
	       <img class="vrtx-thumbnail-image" src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	   <#else>
	     <a href="${imageListing.urls[image.URI]?html}">
	       <img class="vrtx-thumbnail-image" src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	   </#if>
	 <#else>
	   <#if imageListing != "">
	     <#if (image_index == 0) >
	       <a href="${imageListing.urls[image.URI]?html}" class="active">
	         <img class="vrtx-thumbnail-image" src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	     <#else>
	       <a href="${imageListing.urls[image.URI]?html}">
	         <img class="vrtx-thumbnail-image" src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	     </#if>
	   <#else>
	     <#if (image_index == 0) >
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery" class="active">
              <img class="vrtx-thumbnail-image" src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" />
         <#else>
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery">
              <img class="vrtx-thumbnail-image" src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" /> 
         </#if>
	   </#if>
	 </#if>
	        <#if imageListing != "">
	          <span><img class="vrtx-full-image" src="${imageListing.urls[image.URI]?html?split("?")[0]}" alt="${description}" title="${title}" style="width: ${width}px; height: ${height}px" /></span>
	        <#else>  
	          <span><img class="vrtx-full-image" src="${image.URI}" alt="${description}" title="${title}" style="width: ${width}px; height: ${height}px" /></span>
-           </#if>
	        </a>
      </li>
    <#local count = count+1 />
  </#list>
</#macro>