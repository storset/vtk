<#macro printPropertyEditView title inputFieldName value="true" tooltip="" classes="">
<div class="vrtx-radio ${classes}">
  <div><label for="${inputFieldName}">${title}</label></div>
  <div>
      <input name="${inputFieldName}" id="${inputFieldName}-true" type="radio" value="true" <#if value == "true"> checked="checked" </#if> />
      <label for="${inputFieldName}-true">True</label> 
      <input name="${inputFieldName}" id="${inputFieldName}-false" type="radio" value="false" <#if value != "true"> checked="checked" </#if> />
      <label for="${inputFieldName}-false">False</label> 
  </div>
</div>
</#macro>