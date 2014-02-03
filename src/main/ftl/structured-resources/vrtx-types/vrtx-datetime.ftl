<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
  <div class="vrtx-string ${classes}">
    <label for="${inputFieldName}">${title}</label>
    <input size="12" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value?html}" class="inputfield vrtx-textfield date" />
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</#macro>