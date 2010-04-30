<#ftl strip_whitespace=true>

<#assign locale = springMacroRequestContext.getLocale() />

<#if tabMessagePublishPermissionState?exists>
  <div class="tabMessagePublishPermission">
     <#if tabMessagePublishPermissionState?exists && tabMessagePublishPermissionPublish?exists>
       ${tabMessagePublishPermissionState?html}:
       <span id="vrtx-${tabMessagePublishPermissionPublish?html?lower_case}-message">${tabMessagePublishPermissionPublish?html}</span>&nbsp;
     </#if>
     <span id="vrtx-${locale?html?lower_case}-permission">${tabMessagePublishPermissionPermission?html}</span>
  </div>
</#if>

<#if tabMessage?exists> <#-- the general one -->
  <div class="tabMessage">${tabMessage?html}</div>
</#if>