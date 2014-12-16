<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/lib/vtk.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

<#assign resource = resourceContext.currentResource />
<#assign lastModified = resource.getLastModified() />
<#assign modifiedBy = resource.getModifiedBy() />
<span id="server-now-time" class="hidden-server-info">${nowTime?string("yyyy")},${nowTime?string("MM")},${nowTime?string("dd")},${nowTime?string("HH")},${nowTime?string("mm")},${nowTime?string("ss")}</span>
<span id="resource-last-modified" class="hidden-server-info">${lastModified?string("yyyy")},${lastModified?string("MM")},${lastModified?string("dd")},${lastModified?string("HH")},${lastModified?string("mm")},${lastModified?string("ss")}</span>
<span id="resource-last-modified-by" class="hidden-server-info">${modifiedBy?html}</span>
<#if resource.lock?exists && resource.lock.principal?exists>
  <#assign lockedBy = resource.lock.principal.name />
  <#if resource.lock.principal.URL?exists>
    <#assign lockedBy = resource.lock.principal.description />
  </#if>
  <#assign owner = resource.lock.principal.qualifiedName />
  <#assign currentPrincipal = resourceContext.principal.qualifiedName />
  <span id="resource-locked-by-other" class="hidden-server-info"><#if owner?exists && owner != currentPrincipal>true<#else>false</#if></span>
  <span id="resource-locked-by" class="hidden-server-info">${lockedBy?html}</span>
</#if>
<span id="resource-can-edit" class="hidden-server-info"><#if (writePermissionAtAll.permissionsQueryResult)?exists && writePermissionAtAll.permissionsQueryResult = 'true'>true<#else>false</#if></span>

<#include "/system/system.ftl" />

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
    
    <div id="resource-title" class="<@vrtx.resourceToIconResolver resource /> ${resource.collection?string}">
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
