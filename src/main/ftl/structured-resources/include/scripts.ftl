<#import "/lib/autocomplete.ftl" as autocomplete />
<#import "/lib/showhide.ftl" as showhide />

<#macro includeScripts scripts>
  <#local containsAutoCompleteScripts = containsScripts(scripts, 'AUTOCOMPLETE') />
  <#local containsShowHideScripts = containsScripts(scripts, 'SHOWHIDE') />
  <#if containsAutoCompleteScripts>
    <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
  </#if>
  <#if containsShowHideScripts>
    <@showhide.addShowHideScripts srcBase="${webResources?html}"/>
  </#if>
  <script type="text/javascript">
    $(document).ready(function() {
      <#list scripts as script>
        <#if script.type == 'AUTOCOMPLETE' >
          <@autocomplete.addAutocomplete script/>
        <#elseif script.type == 'SHOWHIDE' >
          <@showhide.addShowHide script />  	
        </#if>
      </#list>
    });
  </script>
</#macro>

<#function containsScripts scripts scriptType>
  <#list scripts as script>
    <#if script.type == scriptType >
      <#return true />
    </#if>
  </#list>
  <#return false />
</#function>