<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
  <div class="vrtx-string ${classes}">
    <label for="${inputFieldName}">${title}</label>
    <div class="inputfield vrtx-textfield">
      <input size="20" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value?html}" class="date" />
    </div>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</#macro>