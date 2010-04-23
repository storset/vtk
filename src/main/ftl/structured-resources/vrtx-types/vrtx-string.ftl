<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" inputFieldSize=20 valuemap="" dropdown=false>
<div class="vrtx-string ${classes}">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
  <#if dropdown && valuemap?exists && valuemap?is_hash>
  <select name="${inputFieldName}">
    <#list valuemap?keys as key>
    <#if key = "range">
      <#local rangeList = valuemap[key] />
      <#list rangeList as rangeEntry >
        <option value="${rangeEntry?html}" <#if value == rangeEntry?string> selected </#if>>${rangeEntry}</option>
      </#list>
    <#else>
      <option value="${key?html}" <#if value == key> selected <#else><#if key == "undefined"> selected </#if></#if>>${valuemap[key]}</option>
    </#if>
	</#list>
	</select>
  <#else>
	<input size="${inputFieldSize}" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value?html}"/>
  </#if>
  <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</div>
</#macro>
