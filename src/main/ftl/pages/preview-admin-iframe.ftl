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
    <script type="text/javascript"><!--
      var previewLoadingMsg = "${vrtx.getMsg('preview.loadingMsg')}";
    // --> 
    </script> 
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

    <!-- Preview mode -->
    <ul id="preview-mode">
      <li class="active-mode"><span id="preview-mode-normal">${vrtx.getMsg("preview.view-mode.normal")}</span></li>
      <li><a id="preview-mode-mobile" href="javascript:void(0);">${vrtx.getMsg("preview.view-mode.mobile")}</a></li>
    </ul>
    <script type="text/javascript"><!--
      var fullscreenToggleOpen = '${vrtx.getMsg("preview.actions.fullscreen-toggle.open")}',
          fullscreenToggleClose = '${vrtx.getMsg("preview.actions.fullscreen-toggle.close")}';
    // -->
    </script>
    
    <!-- Preview actions -->
    <#assign mailSubject = resourceContext.currentResource.title?url('UTF-8') />  
    <ul id="preview-actions">
      <li><a id="preview-actions-share" href="mailto:?subject=${mailSubject}&amp;body=${vrtx.getMsg('preview.actions.share.mail.body', '', ['${resourceContext.currentServiceURL?url("UTF-8")}', '${resourceContext.principal.description?url("UTF-8")}'])}">${vrtx.getMsg("preview.actions.share")}</a></li>
      <li><a id="preview-actions-print" href="javascript:void(0);">${vrtx.getMsg("preview.actions.print")}</a></li>
      <li><a id="preview-actions-fullscreen-toggle" href="javascript:void(0);">${vrtx.getMsg("preview.actions.fullscreen-toggle.open")}</a></li>
    </ul>

    <div id="previewIframeWrapper">
      <a href='javascript:void(0);' id='preview-mode-mobile-rotate-hv'>${vrtx.getMsg("preview.actions.mobile.rotate")}</a>
      <span id="previewIframeMobileBg"></span>
      <iframe class="preview" name="previewIframe" id="previewIframe" src="${url}" marginwidth="0" marginheight="0" scrolling="auto" frameborder="0" style="overflow:visible; width:100%; ">
        [Your user agent does not support frames or is currently configured
        not to display frames. However, you may visit
        <a href="${resourceReference}">the related document.</a>]
      </iframe>
    </div>
  </body>
</html>


