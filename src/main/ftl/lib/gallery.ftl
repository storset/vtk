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
    $(document).ready(function() {
      $(".vrtx-image-listing-include").addClass("loading");
    });
    $(window).load(function() {	
	  var wrapper = ".vrtx-image-listing-include";	
	  var container = ".vrtx-image-listing-include-container";	  
	  var options = {
	    fadeInOutTime : ${fadeEffect}
	  }
	  loadImageErrorMsg = "${vrtx.getMsg('imageListing.loading-image.error')}";
	  loadImageMsg = "${vrtx.getMsg('imageListing.loading-image')}";
	  showFullscreen = "${vrtx.getMsg('imageListing.fullscreen.show')}";
	  closeFullscreen = "${vrtx.getMsg('imageListing.fullscreen.close')}";
	  
	  $(wrapper + " li a").vrtxSGallery(wrapper, container, ${maxWidth}, options);			  
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
  <script type="text/javascript">
    var imageUrlsToBePrefetched = [];
  </script>
  
  <#list images as imageEntry>
    <#local image = imageEntry.propertySet />
    <#local description = vrtx.propValue(image, 'image-description')?html?replace("'", "&#39;") />
    <#local title = vrtx.propValue(image, 'title')?html?replace("'", "&#39;") />
    <#local width = vrtx.propValue(image, 'pixelWidth') />
    <#local height = vrtx.propValue(image, 'pixelHeight') />
    <#local photographer = vrtx.propValue(image, "photographer")?html?replace("'", "&#39;") />
    
    <#if height != "" && width != "">
      <#local width = width?number />
      <#local height = height?number />
      <#local fullWidth = width />
      <#local fullHeight = height />
      
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
    <#elseif count % 4 == 0 && count % 6 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-four vrtx-thumb-last-six">
    <#elseif count % 4 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-four">
    <#elseif count % 5 == 0>
      <li class="vrtx-thumb-last vrtx-thumb-last-five">
    <#elseif count % 6 == 0>
      <li class="vrtx-thumb-last-six">
    <#else>
      <li>
    </#if>
    
    <#assign showTitle = false />
    <#if (image.name != title && title != "")>
      <#assign showTitle = true />
    </#if>
    
    <#if photographer != "">
      <#local description = description + " ${vrtx.getMsg('imageAsHtml.byline')}: " + photographer + "." />
    </#if>

    <#assign url = imageEntry.url />
	<#if imageListing != "">
	   <#if ((activeImage == "" && imageEntry_index == 0) || (activeImage != "" && activeImage == url)) >
	     <a href="${url?html}" class="active">
	       <img class="vrtx-thumbnail-image" src="${url.protocolRelativeURL()?html}?vrtx=thumbnail" alt='${description}' <#if showTitle>title="${title}"</#if> />
	       <span><img class="vrtx-full-image" src="${url.protocolRelativeURL()?split("?")[0]?html}" alt='${description}' /></span>
	   <#else>
	     <a href="${url?html}">
	       <img class="vrtx-thumbnail-image" src="${url.protocolRelativeURL()?html}?vrtx=thumbnail" alt='${description}' <#if showTitle>title="${title}"</#if> />
	   </#if>
	 <#else>
	   <#assign finalFolderUrl = vrtx.relativeLinkConstructor(folderUrl, 'viewService') />
	   <#if !finalFolderUrl?ends_with("/")>
	     <#assign finalFolderUrl = finalFolderUrl + "/" /> 
	   </#if>
	   <#if (imageEntry_index == 0) >
          <a href="${finalFolderUrl}?actimg=${url?html}&amp;display=gallery" class="active">
            <img class="vrtx-thumbnail-image" src="${url.protocolRelativeURL()?html}?vrtx=thumbnail" alt='${description}' <#if showTitle>title="${title}"</#if> />
            <span><img class="vrtx-full-image" src="${url.protocolRelativeURL()?html}" alt='${description}' /></span>
       <#else>
         <a href="${finalFolderUrl}?actimg=${url?html}&amp;display=gallery">
            <img class="vrtx-thumbnail-image" src="${url.protocolRelativeURL()?html}?vrtx=thumbnail" alt='${description}' <#if showTitle>title="${title}"</#if> /> 
       </#if>
	 </#if> 
	      <script type="text/javascript"><!--
	        imageUrlsToBePrefetched.push({url: <#if imageListing != "">'${url.protocolRelativeURL()?split("?")[0]?html}'<#else>'${url.protocolRelativeURL()?html}'</#if>, width: '${width}', height: '${height}', fullWidth: '${fullWidth}', fullHeight: '${fullHeight}', alt: '${description?js_string}', title: <#if showTitle>'${title?js_string}'<#else>''</#if>});
	      // -->
	      </script>
	    </a>    
      </li>
    <#local count = count+1 />
  </#list>
</#macro>