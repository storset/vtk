<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="">
<div class="vrtx-string ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
    <input size="20" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value}" class="date" />
  </div>
  <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
</div>
</#macro>