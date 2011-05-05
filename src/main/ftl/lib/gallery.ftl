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

<#macro galleryListImages images activeImage="" imageListing="">
  <#assign count = 1 />
  <#list images as image>
    <#assign description = vrtx.propValue(image, 'description', '', 'content')?html />
    <#assign title = vrtx.propValue(image, 'title')?html />
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
	       <img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	   <#else>
	     <a href="${imageListing.urls[image.URI]?html}">
	       <img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	   </#if>
	 <#else>
	   <#if imageListing != "">
	     <#if (image_index == 0) >
	       <a href="${imageListing.urls[image.URI]?html}" class="active">
	         <img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	     <#else>
	       <a href="${imageListing.urls[image.URI]?html}">
	         <img src="${vrtx.relativeLinkConstructor(image.URI.toString(), 'displayThumbnailService')}" alt="${description}" title="${title}" />
	     </#if>
	   <#else>
	     <#if (image_index == 0) >
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery" class="active">
              <img src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" />
         <#else>
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery">
              <img src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" /> 
         </#if>
	   </#if>
	 </#if>
	       <#if imageListing != "">
	         <span><img src="${imageListing.urls[image.URI]?html}" alt="${description}" title="${title}" /></span>
	       <#else>  
	         <span><img src="${image.URI}" alt="${description}" title="${title}" /></span>
-          </#if>
	       </a>
       </li>
    <#assign count = count+1 />
  </#list>
</#macro>