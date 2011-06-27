<#-- Adds the default required scripts necessary to use show and hide functionality -->

<#macro addShowHideScripts srcBase>
  <script type="text/javascript" src="${srcBase}/editor-showhide.js"></script>
</#macro>

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
  setShowHide('${script.name}', [${parameters}]);
</#macro>