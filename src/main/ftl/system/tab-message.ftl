<#ftl strip_whitespace=true>

<#assign locale = springMacroRequestContext.getLocale() />

<#if tabMessagePublishPermissionState?exists>
  <div class="tabMessagePublishPermission">
     <#assign t = tabMessagePublishPermissionResourceType />
     <#if t == 'structured-article' || t == 'structured-event' ||
     t == 'structured-project' || t == 'person' || t == 'structured-document' ||
     t == 'frontpage' || t == 'featured-content'> <#-- only show published on a chosen "few" -->
       ${tabMessagePublishPermissionState?html}:
       <span id="vrtx-${tabMessagePublishPermissionPublish?html?lower_case}-message">${tabMessagePublishPermissionPublish?html}</span>&nbsp;
     </#if>
     <span id="vrtx-${locale?html?lower_case}-permission">${tabMessagePublishPermissionPermission?html}</span>
  </div>
</#if>

<#if tabMessage?exists> <#-- the general one -->
  <div class="tabMessage">${tabMessage?html}</div>
</#if>