<#ftl strip_whitespace=true>

<#--
  - File: preview-admin-iframe.ftl
  - 
  - Description: A HTML page with a <iframe> tag to the previewed resource
  -              Loads from the admin domain. The src of the iframe points to the
  -              corresponding view domain.
  - 
  - Dynamic resizing of iframe only works in IE and Firefox. 
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
<#if !permissions_ACTION_READ?exists>
  <#stop "Unable to render model: required submodel
  'permissions_ACTION_READ' missing">
</#if>
<#if !permissions_ACTION_READ_PROCESSED?exists>
  <#stop "Unable to render model: required submodel
  'permissions_ACTION_READ_PROCESSED' missing">
</#if>
<#if !enableSelectiveProtocols?exists>
  <#stop "Unable to render model: required submodel
  'enableSelectiveProtocols' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${(title.title)?default(resourceContext.currentResource.name)}</title>
  </head>
  <body id="vrtx-preview">

    <#assign previewRefreshParameter = 'outer-iframe-refresh' />
    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#if !previewViewParameter?exists>
      <#assign previewViewParameter = 'vrtx=previewViewIframe' />
    </#if>
    
	<#if previewImage?exists >
		<#assign url = previewImage.URL />
	<#elseif resourceReference?exists >
    	<#assign url = resourceReference />	  
    </#if>
    
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewViewParameter />
    <#else>
      <#assign url = url + "?" + previewViewParameter />
    </#if>
    <#assign url = url + "&amp;" + previewRefreshParameter + "=" + dateStr />
    
    <#-- Do not show preview if resource is "Allowed for all" and we are on https. Should not normally happen -->
    <#if ((permissions_ACTION_READ.permissionsQueryResult = 'true') || 
          (permissions_ACTION_READ_PROCESSED.permissionsQueryResult = 'true')) 
         && (permissions_ACTION_READ.requestScheme = 'https')
         && (enableSelectiveProtocols = 'true')>
      <p class="previewUnavailable">${vrtx.getMsg("preview.httpOnly")}</p>
    
    <#else>
      <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" vspace="0" hspace="0" style="overflow:visible; width:100%; ">
        [Your user agent does not support frames or is currently configured
        not to display frames. However, you may visit
        <a href="${resourceReference}">the related document.</a>]
      </iframe>

    </#if>

  </body>
</html>


