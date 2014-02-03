<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

  
<noscript>
  <div class="message infomessage">${vrtx.getMsg("msg.browser.javascript-off")}</div>
</noscript>

<#assign resource = resourceContext.currentResource />
<#assign lang = vrtx.getMsg("eventListing.calendar.lang", "en") />

<#assign lastModified = resource.getLastModified() />
<#assign modifiedBy = resource.getModifiedBy() />
<span id="server-now-time" class="hidden-server-info">${nowTime?string("yyyy")},${nowTime?string("MM")},${nowTime?string("dd")},${nowTime?string("HH")},${nowTime?string("mm")},${nowTime?string("ss")}</span>
<span id="resource-last-modified" class="hidden-server-info">${lastModified?string("yyyy")},${lastModified?string("MM")},${lastModified?string("dd")},${lastModified?string("HH")},${lastModified?string("mm")},${lastModified?string("ss")}</span>
<span id="resource-last-modified-by" class="hidden-server-info">${modifiedBy?html}</span>
<#if resourceContext.currentResource.lock?exists && resourceContext.currentResource.lock.principal?exists>
  <#assign lockedBy = resourceContext.currentResource.lock.principal.name />
  <#if resourceContext.currentResource.lock.principal.URL?exists>
    <#assign lockedBy = resourceContext.currentResource.lock.principal.description />
  </#if>
  <span id="resource-locked-by" class="hidden-server-info">${lockedBy?html}</span>
</#if>
<script type="text/javascript"><!--
  if(vrtxAdmin.isIE7 || vrtxAdmin.isIETridentInComp) {
    if(vrtxAdmin.isIETridentInComp) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.comp")}';   
    } else if(vrtxAdmin.isIE6) {   
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie7")}';    
    } else if(vrtxAdmin.isIE7) {
      var outdatedBrowserText = '${vrtx.getMsg("msg.browser.msie.msie6")}';
    }
  }
  var cancelI18n = '${vrtx.getMsg("editor.cancel")}',
      datePickerLang = "${lang}",
      loadingSubfolders = '${vrtx.getMsg("manage.load-subfolders")}',
      uploadingFiles = '${vrtx.getMsg("uploading-files")}',
      publishing = {
        msg: {
          error: {
            unpublishDateBefore: '${vrtx.getMsg("publishing.edit.invalid.unpublishDateBefore")}',
            unpublishDateNonExisting: '${vrtx.getMsg("publishing.edit.invalid.unpublishDateNonExisting")}'
          }
        }
      };
  
  vrtxAdmin.serverFacade.errorMessages = {
    title: "${vrtx.getMsg('ajaxError.title')}", 
    general: "${vrtx.getMsg('ajaxError.general')}",
    timeout: "${vrtx.getMsg('ajaxError.timeout')}",
    abort: "${vrtx.getMsg('ajaxError.abort')}",
    parsererror: "${vrtx.getMsg('ajaxError.parsererror')}", 
    offline: "${vrtx.getMsg('ajaxError.offline')}",
    lockStolen: "${vrtx.getMsg('ajaxError.lockStolen')}",
    lockStolenTitle: "${vrtx.getMsg('ajaxError.lockStolen.title')}",
    lockStolenOk: "${vrtx.getMsg('ajaxError.lockStolen.ok')}",
    outOfDate: "${vrtx.getMsg('ajaxError.out-of-date')}",
    outOfDateTitle: "${vrtx.getMsg('ajaxError.out-of-date.title')}",
    outOfDateOk: "${vrtx.getMsg('ajaxError.out-of-date.ok')}",
    cantBackupFolderTitle: "${vrtx.getMsg('ajaxError.cant-backup-folder.title')}",
    cantBackupFolder: "${vrtx.getMsg('ajaxError.cant-backup-folder')}",
    uploadingFilesFailedTitle: "${vrtx.getMsg('ajaxError.uploading-files.title')}",
    uploadingFilesFailed: "${vrtx.getMsg('ajaxError.uploading-files')}",
    sessionInvalid: "${vrtx.getMsg('ajaxError.sessionInvalid')}",
    sessionInvalidTitle: "${vrtx.getMsg('ajaxError.sessionInvalid.title')}",
    sessionInvalidOk: "${vrtx.getMsg('ajaxError.sessionInvalid.ok')}",
    sessionInvalidOkInfo: "${vrtx.getMsg('ajaxError.sessionInvalid.ok.info')}",
    sessionWaitReauthenticate: "${vrtx.getMsg('ajaxError.sessionInvalid.waitReauthenticate')}",
    sessionValidated: "${vrtx.getMsg('ajaxError.sessionValidated')}",
    sessionValidatedTitle: "${vrtx.getMsg('ajaxError.sessionValidated.title')}",
    sessionValidatedOk: "${vrtx.getMsg('ajaxError.sessionValidated.ok')}",
    down: "${vrtx.getMsg('ajaxError.down')}",
    s500: "${vrtx.getMsg('ajaxError.s500')}",
    s400: "${vrtx.getMsg('ajaxError.s400')}",
    s401: "${vrtx.getMsg('ajaxError.s401')}",
    s403: "${vrtx.getMsg('ajaxError.s403')}",
    s404: "${vrtx.getMsg('ajaxError.s404')}",
    s423: "${vrtx.getMsg('ajaxError.s423')}",
    s4233: "${vrtx.getMsg('ajaxError.s423.parent')}", 
    customTitle: {
      "0": "${vrtx.getMsg('ajaxError.offline.title')}",
      "4233": "${vrtx.getMsg('ajaxError.s423.parent.title')}"
    }
  };
  if(vrtxAdmin.hasFreeze) { // Make immutable
    Object.freeze(vrtxAdmin.serverFacade.errorMessages);
  }
// -->
</script>

<#if pingURL?? && !resourceContext.currentServiceName?lower_case?contains("preview")>
  <@ping.ping url=pingURL['url'] interval=300/> 
</#if>

<#if resource?exists && resourceMenuLeft?exists && resourceMenuRight?exists>
  <@gen resource resourceMenuLeft resourceMenuRight />
<#elseif resource?exists && resourceMenuLeft?exists>
  <@gen resource resourceMenuLeft />
<#else>
  <@gen resource /> 
</#if>

<#macro gen resource resourceMenuLeft="" resourceMenuRight="">
  <div id="title-container">

    <#-- Compact when no items in resourceLeftMenu and no items with buttons (taking more v.space) in resourceMenuRight -->
    <#local compactClass = "" />
    
    <#if (resourceMenuLeftServicesLinkable?? && resourceMenuRightServicesLinkable??)>
      <#if (resourceMenuLeftServicesLinkable == 0 
         && (writePermission.permissionsQueryResult = 'false' || resourceMenuRightServicesLinkable == 0))
         && !(resourceMenuRightServicesLinkable >= 1 && unlockPermission.permissionsQueryResult = 'true' && writePermission.permissionsQueryResult = 'false' && !publishLink.url?? && !unpublishLink.url??)>
        <#local compactClass = " compact" />
      </#if>
    </#if>
    
    <div id="resource-title" class="<@vrtx.resourceToIconResolver resource /> ${resource.collection?string}${compactClass}">
      <h1><#compress>
        <#if resource.URI == '/'>
          ${repositoryID?html}
        <#else>
          ${resource.name?html}
        </#if>
      </#compress></h1>
      <#if browseURL?exists && editField?exists><#-- TODO: fix this hack for browse -->
        <ul class="list-menu button-row" id="resourceMenuLeft">
          <li class="createLinkToResourceService first last">
            <a href="javascript:updateParent('${editField}', '${browseURL}')"><@vrtx.msg code="resourceMenuLeft.createLinkToResourceService" default="Create link" /></a>
          </li>
        </ul>
      </#if>

      <#if resourceMenuLeft != "">
        <@listMenu.listMenu menu=resourceMenuLeft displayForms=true prepend="" append=""/>
      </#if>
      <#if resourceMenuRight != "">
        <@listMenu.listMenu menu=resourceMenuRight displayForms=true prepend="" append=""/>
      </#if>
    </div>
  </div>
</#macro>