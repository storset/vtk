<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

<#assign resource = resourceContext.currentResource />
<#assign lang = vrtx.getMsg("eventListing.calendar.lang", "en") />

<#assign lastModified = resource.getLastModified() />
<#assign modifiedBy = resource.getModifiedBy() />
<span id="resource-last-modified" class="hidden-server-info">${lastModified?string("yyyy")},${lastModified?string("MM")},${lastModified?string("dd")},${lastModified?string("HH")},${lastModified?string("mm")},${lastModified?string("ss")}</span>
<span id="resource-last-modified-by" class="hidden-server-info">${modifiedBy}</span>

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
    outOfDate: "${vrtx.getMsg('ajaxError.out-of-date')}",
    outOfDateTitle: "${vrtx.getMsg('ajaxError.out-of-date.title')}",
    outOfDateRefreshOk: "${vrtx.getMsg('ajaxError.out-of-date.refresh')}",
    outOfDateOverwriteOk: "${vrtx.getMsg('ajaxError.out-of-date.overwrite')}",
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
    customTitle: {
      "0": "${vrtx.getMsg('ajaxError.offline.title')}"
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
        <ul class="list-menu" id="resourceMenuLeft">
          <li class="createLinkToResourceService first last">
            <a href="javascript:updateParent('${editField}', '${browseURL}')"><@vrtx.msg code="resourceMenuLeft.createLinkToResourceService" default="Create link" /></a>
          </li>
        </ul>
      </#if>

      <#if resourceMenuRight != "">
        <@listMenu.listMenu menu=resourceMenuRight displayForms=true prepend="" append=""/>
      </#if>
      <#if resourceMenuLeft != "">
        <@listMenu.listMenu menu=resourceMenuLeft displayForms=true prepend="" append=""/>
      </#if>
    </div>
  </div>
</#macro>