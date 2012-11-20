<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("publish.header") />
<#assign titleMsg = vrtx.getMsg("publish.title") />
<#assign actionURL = item.url />

<h3>${headerMsg}</h3>
<p><span class="unpublished"><@vrtx.msg code="publish.permission.unpublished" /></span></p>
<#if writePermission.permissionsQueryResult = 'true'>
  <a id="vrtx-publish-document" class="vrtx-button-small" title="${titleMsg}" href="${actionURL?html}"><span>${item.title?html}</span></a>
<#else>
  <#-- Not READ_WRITE (not considering locks) and have READ_WRITE_UNPUBLISHED (considering locks) -->
  <#if writeUnlockedPermission.permissionsQueryResult?? && writeUnlockedPermission.permissionsQueryResult = 'false'
    && writeUnpublishedPermission.permissionsQueryResult?? && writeUnpublishedPermission.permissionsQueryResult = 'true'>
    <#assign uri = vrtx.relativeLinkConstructor(resourceContext.currentResource.URI, "emailApprovalService") />
    <#if uri?has_content> 
      <a id="vrtx-send-to-approval-global" title="${vrtx.getMsg('send-to-approval.title')}" class="vrtx-button-small" href="${uri?html}"><span>${vrtx.getMsg('send-to-approval.title')}</span></a>
    </#if>
  </#if>
</#if>

<#recover>
${.error}
</#recover>
