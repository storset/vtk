<#ftl strip_whitespace=true>
<#-- Show and hide functionality -->

<#macro addShowHide script>
  <#local parameters = '' />
  <#list script.params?keys as param>
   	<#list script.params[param] as value >
   	  <#if parameters == ''>
   	    <#local parameters = "'" + value?string + "'"/>
   	  <#else>
   	    <#local parameters = parameters + ", '" + value?string + "'" />
   	  </#if>
   	</#list>
  </#list>
  setShowHide('${script.name}', [${parameters}], true);
</#macro>