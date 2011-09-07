<#ftl strip_whitespace=true>

<#assign locale = springMacroRequestContext.getLocale() />

<#if tabMessagePublishPermissionPermission?exists>
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

<#-- XXX: remove -->
<#if expiresSec?exists && expiresSec["expires-sec"]?exists>
  <#assign delay = expiresSec["expires-sec"]?number / 60>
  <#if delay &gt;= 5>
  <div class="tabMessage">
    <#assign delay = delay?string("0.###")>
    <@vrtx.msg "headerControl.expiresSec",
    "This resource uses the expires property", [delay] />
  </div>
  </#if>
</#if>