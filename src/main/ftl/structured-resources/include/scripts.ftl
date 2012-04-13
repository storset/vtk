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
      <#if containsMultipleInputFieldScripts>
        $.when(MULTIPLE_INPUT_FIELD_TEMPLATES_DEFERRED).done(function() {
          browseBase = '${fckeditorBase.url?html}';
          browseBaseFolder = '${baseFolder}';
          browseBasePath = '${fckBrowse.url.pathRepresentation}';
          <#list scripts as script>
            <#if script.type == 'MULTIPLEINPUTFIELDS' >
              loadMultipleInputFields('${script.name}', '${vrtx.getMsg("editor.add")}', '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}', 
                                      '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', true, false);
            </#if>
          </#list>
        });
      </#if>
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
