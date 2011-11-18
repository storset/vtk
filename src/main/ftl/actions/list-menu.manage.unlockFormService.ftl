<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("actions.lockedBy") />
<#assign titleMsg = vrtx.getMsg("manage.unlock.title") />

<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=110&width=250' />
  <#assign method = "get" />
</#if>

<#assign lockedBy = resourceContext.currentResource.lock.principal.name />
  <#if resourceContext.currentResource.lock.principal.URL?exists>
    <#assign lockedBy  = '<a href="' + resourceContext.currentResource.lock.principal.URL + '">' 
                       + resourceContext.currentResource.lock.principal.description + '</a>' />
</#if>

<h3>${headerMsg}</h3>
<p>${lockedBy}</p>
<#if writePermission.permissionsQueryResult = 'true'>
<a id="vrtx-unlock" class="vrtx-button-small vrtx-admin-button" title="${titleMsg}" href="${actionURL?html}"><span>${item.title?html}</span></a>
</#if>
<#recover>
${.error}
</#recover>
