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
         // ("load") so that all images is loaded before running,
	     // and .bind for performance increase: http://jqueryfordesigners.com/demo/fade-method2.html
		 $(window).bind("load", function () {
				
		   var wrapper = ".vrtx-image-listing-include";	
		   var container = ".vrtx-image-listing-include-container";
			  
		   var options = {
		     fadeInOutTime : ${fadeEffect}
		   }
			  
		   $(wrapper + " ul li a").vrtxSGallery(wrapper, container, 507, options);
				  
	     });
       // -->
       </script>
      <#list images as image>
        <div class="vrtx-image-listing-include-container-pure-css">
          <div class="vrtx-image-listing-include-container-nav-pure-css">
            <a class="prev" href="#" title="&lt;&lt; Forrige"><span class="prev-transparent-block"></span></a>
            <a class="next" href="#" title="Neste &gt;&gt;"><span class="next-transparent-block"></span></a>
          </div>
          <a class="vrtx-image-listing-include-container-link" href="${folderUrl}?actimg=${image.URI}&amp;display=gallery">
            <img src="${image.URI}" alt="${image.URI}" />
          </a>
        </div>
        <#break />
      </#list>
    </#if>
    <#if type == 'gallery'>
      <ul class="vrtx-image-listing-include-thumbs-pure-css">
    <#else>
      <ul class="vrtx-image-listing-include-thumbs">
    </#if>
    <#assign count = 1 />
    <#list images as image>
        <#assign description = vrtx.propValue(image, 'description', '', 'content')?html />
        <#if count % 4 == 0>
          <li class="vrtx-thumb-last">
        <#else>
          <li>
        </#if>
          <#if (image_index == 0) >
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery" class="active"><img src="${image.URI}?vrtx=thumbnail" alt="${description}" />
          <#else>
            <a href="${folderUrl}?actimg=${image.URI}&amp;display=gallery"><img src="${image.URI}?vrtx=thumbnail" alt="${description}" /> 
          </#if>
              <#if type == 'gallery'><span><img src="${image.URI}" alt="${description}" /></span></#if>
            </a>
        </li>
        <#assign count = count+1 />
    </#list>
    </ul>
  </div>
</#if>