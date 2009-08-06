<#import "/lib/autocomplete.ftl" as autocomplete />

<#macro includeScripts scripts>
  <@autocomplete.addAutoCompleteScripts srcBase="${webResources?html}"/>
  <script type="text/javascript">
    $(document).ready(function() {
      <#list scripts as script>
        <#if script.type == 'AUTOCOMPLETE' >
           <@autocomplete.addAutocomplete script/>
        </#if>
      </#list>
    });
  </script>
</#macro>