<#ftl strip_whitespace=true>
<#import "/lib/showhide.ftl" as showhide />
<#import "/lib/multipleinputfields.ftl" as multipleinputfields />

<#macro includeScripts scripts>
  <#local containsShowHideScripts = containsScripts(scripts, 'SHOWHIDE') />
  <#local containsMultipleInputFieldScripts = containsScripts(scripts, 'MULTIPLEINPUTFIELDS') />
  <#if containsShowHideScripts>
    <@showhide.addShowHideScripts srcBase="${jsBaseURL?html}"/>
  </#if>
  <script type="text/javascript"><!--
    $(document).ready(function() {
      <#list scripts as script>
        <#if script.type == 'SHOWHIDE' >
          <@showhide.addShowHide script />
        </#if>
      </#list>
      $.when(MULTIPLE_INPUT_FIELD_TEMPLATES_DEFERRED).done(function() {
        browseBase = '${fckeditorBase.url?html}';
        browseBaseFolder = '${baseFolder}';
        browseBasePath = '${fckBrowse.url.pathRepresentation}';
        <#list scripts as script>
          <#if script.type == 'MULTIPLEINPUTFIELDS' >
            <@multipleinputfields.addMultipleInputFields script />
          </#if>
        </#list>
      });
    });
    //-->
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
