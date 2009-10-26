<#ftl strip_whitespace=true>

<#if tabMessage?exists> <#-- the general one -->
  <div class="tabMessage">${tabMessage?html}</div>
</#if>

<#if tabMessagePublishPermissionState?exists>
  <div class="tabMessagePublishPermission">
     ${tabMessagePublishPermissionState?html}:&nbsp;
     <span id="vrtx-${tabMessagePublishPermissionPublish?html?lower_case}-message">${tabMessagePublishPermissionPublish?html}</span>
     ${tabMessagePublishPermissionPermission?html}
  </div>
</#if>