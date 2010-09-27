<#import "/lib/vortikal.ftl" as vrtx/>

<#if !excludeScripts?exists>
<#if cssURLs?exists>
  <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
  </#list>
</#if>

<#if jsURLs?exists && type == 'gallery'>
  <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
  </#list>
</#if>
</#if>

<#if images?exists>
  <div class="vrtx-image-listing-include">
    <span class="vrtx-image-listing-include-title"><a href="${folderUrl}?display=gallery">${folderTitle}</a></span>
    <#if type == 'gallery'>
       <script type="text/javascript">
       <!--
		 $(window).load(function () {
				
		   var wrapper = ".vrtx-image-listing-include";	
		   var container = ".vrtx-image-listing-include-container";
			  
		   var options = {
		     fadeInOutTime : ${fadeEffect}
		   }
			  
		   $(wrapper + " ul li a").vrtxSGallery(wrapper, container, 507, options);
				  
	     });
       // -->
       </script>
        <div class="vrtx-image-listing-include-container-pure-css">
          <div class="vrtx-image-listing-include-container-nav-pure-css">
            <a class="prev" href="#" title="${vrtx.getMsg('imageListing.previous.prefix')}&nbsp;${vrtx.getMsg('imageListing.previous')}"><span class="prev-transparent-block"></span></a>
            <a class="next" href="#" title="${vrtx.getMsg('imageListing.next')}&nbsp;${vrtx.getMsg('imageListing.next.postfix')}"><span class="next-transparent-block"></span></a>
          </div>
        </div>
    </#if>
    <#if type == 'gallery'>
      <ul class="vrtx-image-listing-include-thumbs-pure-css">
    <#else>
      <ul class="vrtx-image-listing-include-thumbs">
    </#if>
    <#assign count = 1 />
    <#list images as image>
        <#assign description = vrtx.propValue(image, 'description', '', 'content')?html />
        <#-- <#if description?has_content>
          <#if (description?string?length > 97) >
            <#assign description = description?substring(0, 97) + "..." />
          </#if>
        </#if> -->
        <#assign title = vrtx.propValue(image, 'userTitle')?html />
        <#if count % 4 == 0 && count % 5 == 0>
          <li class="vrtx-thumb-last vrtx-thumb-last-four vrtx-thumb-last-five">
        <#elseif count % 4 == 0>
          <li class="vrtx-thumb-last vrtx-thumb-last-four">
        <#elseif count % 5 == 0>
          <li class="vrtx-thumb-last-five">
        <#else>
          <li>
        </#if>
          <#if (image_index == 0) >
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery" class="active"><img src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" />
          <#else>
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery"><img src="${vrtx.linkConstructor(image.URI.toString(), 'displayThumbnailService').getPathRepresentation()}" alt="${description}" title="${title}" /> 
          </#if>
              <#if type == 'gallery'><span><img src="${image.URI}" alt="${description}" title="${title}" /></span></#if>
            </a>
        </li>
        <#assign count = count+1 />
    </#list>
    </ul>
  </div>
</#if>