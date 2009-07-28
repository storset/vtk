
<#macro addAutoCompleteScripts srcBase>
  
  <script type='text/javascript' src='${srcBase}/jquery/jquery.autocomplete.js'></script>
  <script type='text/javascript' src='${srcBase}/jquery/jquery.autocomplete.override.js'></script>
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/jquery.autocomplete.css" />
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/jquery.override.css" />

</#macro>

<#macro createAutoCompleteInputField appSrcBase service id
                                     value=""
                                     minChars=2
                                     multiple="true"
                                     selectFirst="true"
                                     width=""
                                     hasDescription=false
                                     max=20
                                     delay="">

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
        { minChars:${minChars},
          max:${max},
          multiple:${multiple},
          selectFirst:${selectFirst}
          <#if width != "">,
          width:${width}
          </#if><#if delay != "">,
          delay:${delay}
          </#if><#if hasDescription>,
            formatItem: function(data, i, n, value) {
              return value.split(";")[0] + " (" + value.split(";")[1] + ")";
            },
            formatResult: function(data, value) {
              return value.split(";")[0];
            }
          </#if>
        });
    });
  </script>
  <input type="text" id="${id}" name="${id}" value="${value?html}" />
  
</#macro>