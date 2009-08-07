<#import "/lib/autocomplete.ftl" as autocomplete />
<#import "/lib/showhide.ftl" as showhide />

<#macro includeScripts scripts>
  <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
  <@showhide.addShowHideScripts srcBase="${webResources?html}"/>
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