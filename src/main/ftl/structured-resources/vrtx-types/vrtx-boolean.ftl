<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName description value="" tooltip="" classes="" defaultValue="true">
  <#assign locale = springMacroRequestContext.getLocale() />

  
  <#if classes?contains("vrtx-checkbox")>
    <div class="vrtx-checkbox ${classes}">
      <div class="vrtx-checkbox-buttons">
        <input name="${inputFieldName}" id="${inputFieldName}-true" type="checkbox" value="true" <#if value == "true" > checked="checked" </#if> />
        <label for="${inputFieldName}-true">${title}</label> <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
      </div>
    </div>
  <#else>
    <#if value=="" >
      <#local value = defaultValue />
    </#if>
    <div class="vrtx-radio ${classes}"> 
      <label>${title}</label><#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
      <div class="vrtx-radio-buttons">
        <input name="${inputFieldName}" id="${inputFieldName}-true" type="radio" value="true" <#if value == "true" > checked="checked" </#if> />
        <label for="${inputFieldName}-true">${description.getVocabularyValue(locale,"true")}</label> 
        <input name="${inputFieldName}" id="${inputFieldName}-false" type="radio" value="false" <#if value != "true" || value == ""> checked="checked" </#if> />
        <label for="${inputFieldName}-false">${description.getVocabularyValue(locale,"false")}</label> 
      </div>
    </div>
  </#if>
  
</#macro>
