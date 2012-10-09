<#ftl strip_whitespace=true>

<#--
  - File: preview-popup.ftl
  - 
  - Description: 
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title><@vrtx.msg code="preview.popup.title" args=["${(title.title)?default(resourceContext.currentResource.name)}"] /></title>
    <link type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/default.css" rel="stylesheet" />
    <#include "/system/css.ftl"/> 
    <#include "/system/javascript.ftl"/>
    <#-- TODO: externalize more of the script -->
    <script type="text/javascript"><!--
	  function linkCheckResponseLocalizer(status) {
	      switch (status) {
	         case 'NOT_FOUND':
	         case 'MALFORMED_URL':
	            return '<@vrtx.msg code="linkcheck.status.NOT_FOUND" default="broken"/>';
	         case 'TIMEOUT':
	            return '<@vrtx.msg code="linkcheck.status.TIMEOUT" default="timeout"/>';
	         default:
	            return '<@vrtx.msg code="linkcheck.status.ERROR" default="error"/>';
	      }
	  }
	  
	  function linkCheckCompleted(requests, brokenLinks) {
	    if(brokenLinks > 0) {
	      $("body").find("#vrtx-link-check-spinner")
	        .html(brokenLinks + ' <@vrtx.msg code="linkcheck.brokenlinks" default=" broken links"/>')
	        .addClass("vrtx-broken-links");
	    } else {
	      $("body").find("#vrtx-link-check-spinner").remove();
	    }
	  }
	
	  $(document).ready(function() {
	     $('iframe').load(function() {
	        $(this).contents().find("a").attr("target", "_top");
	        
	        <#if visualizeBrokenLinks?exists && visualizeBrokenLinks = 'true'> 
	        
	        $("body").prepend('<span id="vrtx-link-check-spinner"><@vrtx.msg code="linkcheck.spinner" default="Checking links..."/></span>');
	        
	        var linkCheckURL = '${linkcheck.URL?html}';
	        var authTarget = '${authTarget}';
	        var href = location.href;
	        linkCheckURL = (authTarget === "https" && linkCheckURL.match(/^http:\/\//)) ? linkCheckURL.replace("http://", "https://") : linkCheckURL;
	        
	        visualizeBrokenLinks({
	            selection : 'iframe',
	            validationURL : linkCheckURL,
	            chunk : 10,
	            responseLocalizer : linkCheckResponseLocalizer,
	            completed : linkCheckCompleted
	        });
	        </#if>
	     });
	  });	
	  //-->
    </script>
  </head>
  <body id="vrtx-preview-popup">
    <h1><@vrtx.msg code="preview.popup.title" args=["${(title.title)?default(resourceContext.currentResource.name)}"] /></h1>

    <#assign previewRefreshParameter = 'vrtxPreviewForceRefresh' />
    <#assign previewUnpublishedParameter = 'vrtxPreviewUnpublished' />

    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#assign url = resourceReference />
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewUnpublishedParameter + "="  + "true" 
               + "&amp;link-check=" + visualizeBrokenLinks?default('false')
               + "&amp;" + previewRefreshParameter + "=" + dateStr + "&amp;authTarget=" + authTarget />
    <#else>
      <#assign url = url + "?" + previewUnpublishedParameter + "=" + "true"
               + "&amp;link-check=" + visualizeBrokenLinks?default('false')
               + "&amp;" + previewRefreshParameter + "=" + dateStr + "&amp;authTarget=" + authTarget />
    </#if>

    <#if workingCopy?exists>
      <#assign url = url + "&amp;revision=WORKING_COPY" />
      <div id="vrtx-preview-message-box">${vrtx.getMsg('preview.popup.working-copy')}</div>
    <#else>
      <#assign published = vrtx.propValue(resourceContext.currentResource, "published") />
      <#if !published?exists || published = "false">
        <div id="vrtx-preview-message-box">${vrtx.getMsg('preview.popup.unpublised')}</div>
      </#if>
    </#if>


    <iframe class="previewView" name="previewViewIframe" id="previewViewIframe" src="${url?html}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" style="overflow:visible; width:100%; ">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${url?html}">the related document.</a>]
    </iframe>
      

  </body>
</html>
