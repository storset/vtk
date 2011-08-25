<#ftl strip_whitespace=true>
<#-- Adds the default required scripts necessary to use autocomplete functionality -->

<#macro addAutoCompleteScripts srcBase>
  <script type='text/javascript' src='${srcBase}/jquery/plugins/jquery.autocomplete.js'></script>
  <script type='text/javascript' src='${srcBase}/js/autocomplete/autocomplete.js'></script>
  <link rel="stylesheet" type="text/css" href="${srcBase}/jquery/plugins/jquery.autocomplete.css" />
  <link rel="stylesheet" type="text/css" href="${srcBase}/js/autocomplete/autocomplete.override.css" />
</#macro>

<#macro addAutocomplete script>
  <#local serviceId = script.name />
  <#local parameters = '' />
  <#list script.params?keys as param>
    <#if param == 'service'>
      <#local serviceId = script.params[param] />
    <#else>
      <#if parameters == ''>
        <#local parameters = param?string + ":" + script.params[param]?string />
      <#else>
        <#local parameters = parameters + ", " + param?string + ":" + script.params[param]?string />
      </#if>
    </#if>
  </#list>
  setAutoComplete('${script.name}', '${serviceId}', {${parameters}});
</#macro>