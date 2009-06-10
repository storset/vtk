<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" inputFieldSize=20>
<div class="vrtx-string ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
    <input size="${inputFieldSize}" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value}"/>
  </div>
  <div class="tooltip">${tooltip}</div>
</div>
</#macro>