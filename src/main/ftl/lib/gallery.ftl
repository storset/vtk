<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#--
  - File: gallery.ftl
  - 
  - Description: for use in gallery component and gallery folder
  -
  -->

<#macro galleryJSInit fadeEffect>
  <script type="text/javascript"><!--
    $(document).ready(function() {
      $("#vrtx-image-listing-include-${unique}").addClass("loading");
    });
    $(window).load(function() {	
	  var container = ".vrtx-image-listing-include-container";	  
	  $("#vrtx-image-listing-include-${unique}" + " li a").vrtxSGallery("#vrtx-image-listing-include-${unique}", container, "${unique}", {
	    fadeInOutTime : ${fadeEffect},
	    i18n: {
	      showImageDescription: "${vrtx.getMsg('imageListing.description.show')}",
	      hideImageDescription: "${vrtx.getMsg('imageListing.description.hide')}",
	      showFullscreen: "${vrtx.getMsg('imageListing.fullscreen.show')}",
	      showFullscreenResponsive: "${vrtx.getMsg('imageListing.fullscreen.show.responsive')}",
	      closeFullscreen: "${vrtx.getMsg('imageListing.fullscreen.close')}"
	    }
	  });			  
    });
  // -->
  </script>
  <div class="vrtx-image-listing-include-container-pure-css">
    <div class="vrtx-image-listing-include-container-nav-pure-css">
      <a class="prev" href="#" title="${vrtx.getMsg('imageListing.previous.prefix')}&nbsp;${vrtx.getMsg('previous')}"><span class="prev-transparent-block"></span></a>
      <a class="next" href="#" title="${vrtx.getMsg('next')}&nbsp;${vrtx.getMsg('imageListing.next.postfix')}"><span class="next-transparent-block"></span></a>
    </div>
  </div>
</#macro>

<#macro galleryListImages images activeImage="" imageListing="">
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
    <#local fullWidth = vrtx.propValue(image, 'pixelWidth') />
    <#local fullHeight = vrtx.propValue(image, 'pixelHeight') />
    <#local photographer = vrtx.propValue(image, "photographer")?html?replace("'", "&#39;") />
    
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
      <#local description = description + " &lt;p&gt;${vrtx.getMsg('imageAsHtml.byline')}: " + photographer + ".&lt;/p&gt;" />
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