<#ftl strip_whitespace=true>

<#--
  - File: preview-view-iframe.ftl
  - 
  - Description: A HTML page with a <iframe> tag to the previewed resource
  - 
  - Loads from the view domain, so that Javascript contained on this page can
  - manipulate the contents of the iframe (which is the main purpose of having
  - this "extra" iframe).
  - 
  - Processes the contents of the iframe by changing link targets and (optionally)
  - visualizing broken links.
  -
  - Adds a force-refresh parameter with a timestamp to the url to prevent caching 
  - of the contents, and a parameter to display unpublished pages (only makes a difference for 
  - resources that can be published).
  - 
  - Adds authTarget=http to ensure authenctication (e.g. needed to view unpublished). 
  - Can be hardcoded as the value og "http" is only respected for world readable resources 
  - (where preview should in fact be on http) 
  -
  - Directly includes"/system/javascript.ftl" since this 
  - page is not part of admin and therefore not decorated
  -
  - TODO: Hack based on Ny UiO web styles/structure with regard to visualizing broken links should ideally not be here in vortikal
  - 
  - Required model data:
  -   resourceReference
  -   resourceContext
  -  
  - Optional model data:
  -   title
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#if !resourceReference?exists>
  <#stop "Unable to render model: required submodel
  'resourceReference' missing">
</#if>
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel
  'resourceContext' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>

  <#include "/system/javascript.ftl"/>
  
  <script language="javascript" >
  $(document).ready(function(){
  
    <#if visualizeBrokenLinks?exists && visualizeBrokenLinks = 'true'>
  
      var deadLinks = visualizeDeadLinkInit();
    
    </#if>
  
	$('iframe').load(function() {
	
		$("iframe").contents().find("a").each(function(i, e){
  		  this.target = "_top";
		});
		
		<#if visualizeBrokenLinks?exists && visualizeBrokenLinks = 'true'>
		
		  $("iframe").contents().find("body")
            .filter(function() {
              return this.id.match(/^(?!vrtx-[\S]+-listing|vrtx-collection)[\S]+/);
            })
            .find("#main")
            .not("#left-main")
              .find("a").each(function(i, e){
                visualizeDeadLink(this, deadLinks);
              });
            
        </#if>
	});
  });	
  </script>

  </head>
  <body>

    <#if !previewRefreshParameter?exists>
      <#assign previewRefreshParameter = 'vrtxPreviewForceRefresh' />
    </#if>
    
    <#if !previewUnpublishedParameter?exists>
      <#assign previewUnpublishedParameter = 'vrtxPreviewUnpublished' />
    </#if>

    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#assign url = resourceReference />
    <#-- XXX: remove hard-coded 'authTarget' parameter: -->
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewUnpublishedParameter + "=" + "true" + "&amp;" + previewRefreshParameter + "=" + dateStr + "&amp;authTarget=http" />
    <#else>
      <#assign url = url + "?" + previewUnpublishedParameter + "=" + "true" + "&amp;" + previewRefreshParameter + "=" + dateStr + "&amp;authTarget=http" />
    </#if>

    <iframe class="previewView" name="previewViewIframe" id="previewViewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; ">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    </iframe>
  </body>
</html>


