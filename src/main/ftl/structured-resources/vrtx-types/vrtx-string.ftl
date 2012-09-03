<#ftl strip_whitespace=true>
<#macro printPropertyEditView title inputFieldName value="" tooltip="" classes="" inputFieldSize=20 valuemap="" dropdown=false multiple=false defaultValue="">
<div class="vrtx-string ${classes}<#if multiple> vrtx-multiple</#if>">
  <label for="${inputFieldName}">${title}</label>
  <div class="inputfield">
    <#if dropdown && valuemap?exists && valuemap?is_hash && !multiple>
      <#if value=="" >
        <#local value=defaultValue />
      </#if>
      <select name="${inputFieldName}" id="${inputFieldName}">
        <#list valuemap?keys as key>
          <#if key = "range">
            <#local rangeList = valuemap[key] />
            <#list rangeList as rangeEntry >
              <option value="<#if rangeEntry?string != defaultValue>${rangeEntry?html}</#if>" <#if value == rangeEntry?string> selected="selected" </#if>>${rangeEntry}</option>
            </#list>
          <#else>
            <#if value != "">
              <option value="${key?html}" <#if value == key> selected="selected" </#if>>${valuemap[key]}</option>
            <#else>
              <option value="${key?html}" <#if key == "undefined"> selected="selected" </#if>>${valuemap[key]}</option>
            </#if>
          </#if>
	    </#list>
	  </select>
    <#else>
      <#if inputFieldName == "title">
        <div class="vrtx-textfield-big">
      <#else>
        <#if multiple && dropdown && valuemap?exists && valuemap?is_hash>
          <#if value=="" >
            <#local value=defaultValue />
          </#if>
          <div class="vrtx-textfield vrtx-multiple-dropdown">
          <script type="text/javascript"><!--
            var dropdown${inputFieldName} = [
              <#list valuemap?keys as key>
                { 
                  key: "${key?html}",
                  value: "${valuemap[key]}"
                } 
                <#if (key_index < (valuemap?size - 1))>, </#if>
              </#list>
            ];
          // -->
          </script>
        <#else>
          <div class="vrtx-textfield">
        </#if>
      </#if>
	    <input size="${inputFieldSize}" type="text" name="${inputFieldName}" id="${inputFieldName}" value="${value?html}" />
	  </div>
    </#if>
    <#if "${tooltip}" != ""><div class="tooltip">${tooltip}</div></#if>
  </div>
</div>
</#macro>
