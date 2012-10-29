<#ftl strip_whitespace=true>

<#--
  - File: preview-admin-iframe.ftl
  - 
  - Description: A HTML page with a <iframe> tag to the previewed resource
  -              Loads from the admin domain. The src of the iframe points to the
  -              corresponding view domain.
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
<#if !webProtocol?exists>
  <#stop "Unable to render model: required submodel
  'webProtocol' missing">
</#if>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${(title.title)?default(resourceContext.currentResource.name)}</title>  
  </head>
  <body id="vrtx-preview">

    <#if workingCopy?exists || obsoleted?exists>
      <div class="tabMessage-big">
        <#if workingCopy?exists><@vrtx.rawMsg code="preview.workingCopyMsg" args=[versioning.currentVersionURL?html] />
        <#elseif obsoleted?exists><@vrtx.rawMsg code="obsoleted.preview" /></#if>
      </div>
    </#if>
    
    <#assign previewRefreshParameter = 'outer-iframe-refresh' />
    <#assign constructor = "freemarker.template.utility.ObjectConstructor"?new() />
    <#assign dateStr = constructor("java.util.Date")?string("yyyymmddhhmmss") />

    <#if !previewViewParameter?exists>
      <#assign previewViewParameter = 'vrtx=previewViewIframe' />
    </#if>
    
    <#if previewImage?exists >
      <#assign url = previewImage.URL />
      <#-- Hack for image as web page -->
      <#if resourceReference?starts_with("https://") && url?starts_with("http://")>
        <#assign url = url?replace("http://", "https://") />
      </#if>
    <#elseif resourceReference?exists >
      <#assign url = resourceReference />	  
    </#if>
    
    <#if url?contains("?")>
      <#assign url = url + "&amp;" + previewViewParameter />
    <#else>
      <#assign url = url + "?" + previewViewParameter />
    </#if>
    <#assign url = url + "&amp;" + previewRefreshParameter + "=" + dateStr />

    <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" style="overflow:visible; width:100%; ">
      [Your user agent does not support frames or is currently configured
      not to display frames. However, you may visit
      <a href="${resourceReference}">the related document.</a>]
    </iframe>
  </body>
</html>


