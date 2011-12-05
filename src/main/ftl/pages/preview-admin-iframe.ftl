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
    <#if workingCopy?exists>
      <div class="tabMessage-big">
        <@vrtx.msg code="preview.workingCopyMsg" /> <@vrtx.msg code="workingCopyMsgPre" /> <a class="vrtx-revisions-view" href="${resourceContext.currentURI?html}?x-decorating-mode=plain"><@vrtx.msg code="workingCopyMsgPost" /></a>.
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
      <#elseif resourceReference?exists >
      <#assign url = resourceReference />	  
    </#if>
    
    <#-- current URL to use in hash communication with iframe (Opera and IE 7) -->
    <#assign origUrl = url?replace("?vrtx=view-as-webpage", "") + "?vrtx=admin" />
    <#if resourceContext.currentResource.collection>
      <#assign origUrl = origUrl + "&amp;action=preview" />
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
         && (permissions_ACTION_READ.requestScheme = 'https') && (permissions_ACTION_READ.requestPort = 443)
         && (enableSelectiveProtocols = 'true') && (webProtocol = 'http') >
      <p class="previewUnavailable">${vrtx.getMsg("preview.httpOnly")}</p>
    
    <#else>
      <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}#${origUrl}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" style="overflow:visible; width:100%; ">
        [Your user agent does not support frames or is currently configured
        not to display frames. However, you may visit
        <a href="${resourceReference}">the related document.</a>]
      </iframe>
    </#if>

  </body>
</html>


