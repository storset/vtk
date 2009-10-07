<#macro printPropertyEditView title inputFieldName description value="true" tooltip="" classes="">
<#assign locale = springMacroRequestContext.getLocale() />
<div class="vrtx-radio ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div>
      <input name="${inputFieldName}" id="${inputFieldName}-true" type="radio" value="true" <#if value == "true" > checked="checked" </#if> />
      <label for="${inputFieldName}-true">${description.getVocabularyValue(locale,"true")}</label> 
      <input name="${inputFieldName}" id="${inputFieldName}-false" type="radio" value="false" <#if value != "true" || value == ""> checked="checked" </#if> />
      <label for="${inputFieldName}-false">${description.getVocabularyValue(locale,"false")}</label> 
  </div>
</div>
</#macro>