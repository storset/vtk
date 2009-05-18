
<#macro addAutoCompleteScripts srcBase>
  
  <script type='text/javascript' src='${srcBase}/jquery/jquery.autocomplete.js'></script>
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/jquery.autocomplete.css" />

</#macro>

<#macro createAutoCompleteInputField appSrcBase service id value>

  <#-- id might contain '.' (dot) -->
  <#local elementId = "" />  
  <#if id?contains(".")>
    <#list id?split(".") as x>
      <#if elementId = "">
        <#local elementId = x />
      <#else>
        <#local elementId = elementId + "\\\\." + x />
      </#if>
    </#list>
  <#else>
    <#local elementId = id />
  </#if>

  <script type="text/javascript">
    $(document).ready(function() {
      $("#${elementId}").autocomplete('${appSrcBase}?vrtx=admin&action=autocomplete&service=${service}', 
          { minChars:2, multiple:true });
    });
  </script>
  <input type="text" id="${id}" name="${id}" value="${value?html}" />
  
</#macro>