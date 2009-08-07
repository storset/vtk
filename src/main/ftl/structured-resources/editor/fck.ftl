<#import "/lib/vortikal.ftl" as vrtx />

<#macro addFckScripts>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/imageref.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/serverbrowsedialog.js"></script>
  <script language="Javascript" type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
  <script language="Javascript" type="text/javascript" src="${jsBaseURL?html}/admin-fck-setup.js"></script>
</#macro>

<#macro insertEditor content completeEditor=false withoutSubSuper=false>
    <#local baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#local baseFolder = resourceContext.parentURI?html />
    </#if>
    <#local cssFileList = "" />
    <#if fckEditorAreaCSSURL?exists>
      <#list fckEditorAreaCSSURL as cssURL>
        <#if cssFileList == ''>
          <#local cssFileList = cssURL?html />
        <#else>
          <#local cssFileList = cssFileList + ", " + cssURL?html />
        </#if>
      </#list>
    </#if>
    <script language="Javascript" type="text/javascript">
      newEditor('${content}', ${completeEditor?string}, ${withoutSubSuper?string}, 
        '${baseFolder?string}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
        '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', {${cssFileList}});      
    </script>
</#macro>
