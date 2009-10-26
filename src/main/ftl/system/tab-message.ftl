<#ftl strip_whitespace=true>

<#if tabMessage?exists> <#-- the general one -->
  <div class="tabMessage">${tabMessage?html}</div>
</#if>

<#if tabMessagePublishPermissionState?exists>
  <div class="tabMessagePublishPermission">
     <#assign t = tabMessagePublishPermissionResourceType />
     <#if t == 'html' || t == 'article' || t == 'event' ||
     t == 'structured-article' || t == 'structured-event' ||
     t == 'structured-project' || t == 'person' || t == 'structured-document' ||
     t == 'frontpage' || t == 'featured-content'> <#-- only show published on a chosen "few" -->
       ${tabMessagePublishPermissionState?html}:
       <span id="vrtx-${tabMessagePublishPermissionPublish?html?lower_case}-message">${tabMessagePublishPermissionPublish?html}</span>&nbsp;
     </#if>
     ${tabMessagePublishPermissionPermission?html}
  </div>
</#if>