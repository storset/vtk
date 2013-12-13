<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName description value="" tooltip="" classes="" defaultValue="true">
  <#assign locale = springMacroRequestContext.getLocale() />
  <#if value=="" >
  <#local value = defaultValue />
  </#if>
  <div class="vrtx-radio ${classes}">
    <div class="vrtx-radio-buttons">
      <input name="${inputFieldName}" id="${inputFieldName}-true" type="checkbox" value="true" <#if value == "true" > checked="checked" </#if> />
      <label for="${inputFieldName}-true">${title}</label> <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
    </div>
  </div>
</#macro>