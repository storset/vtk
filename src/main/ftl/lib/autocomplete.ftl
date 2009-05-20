
<#macro addAutoCompleteScripts srcBase>
  
  <script type='text/javascript' src='${srcBase}/jquery/jquery.autocomplete.js'></script>
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/jquery.autocomplete.css" />

</#macro>

<#macro createAutoCompleteInputField appSrcBase service id value="" multiple=true width="" minChars=2 hasDescription=false>

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
          { minChars:${minChars}
            <#if multiple>,
              multiple:true
            </#if>
            <#if width != "">,
              width:${width}
            </#if>
            <#if hasDescription>,
              formatItem: function(data, i, n, value) {
                return value.split(";")[0] + " (" + value.split(";")[1] + ")";
              },
              formatResult: function(data, value) {
                return value.split(";")[1];
              }
            </#if>
          });
    });
  </script>
  <input type="text" id="${id}" name="${id}" value="${value?html}" />
  
</#macro>