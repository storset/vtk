<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" inputFieldSize=20 valuemap="" dropdown=false>
<div class="vrtx-string ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
  <#if dropdown>
  <select name="${inputFieldName}">
    <#list valuemap?keys as key>
	<option value="${key?html}" <#if value == key> selected </#if>>${valuemap[key]}</option>
	</#list>
	</select>
  <#else>
	<input size="${inputFieldSize}" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value?html}"/>
  </#if>
  <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</div>
</#macro>
