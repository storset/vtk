<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("copyMove.move-resources.header") />
<#assign titleMsg = vrtx.getMsg("copyMove.move.title") />
<#assign filesI18n = vrtx.getMsg("copyMove.files") />
<#assign filesTipI18n = vrtx.getMsg("copyMove.files.move.tip.title") /> 
<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=110&width=250' />
  <#assign method = "get" />
</#if>

<h3>${headerMsg}</h3>
<#if session.filesToBeCopied?exists>
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/plugins/jquery.vortexTips.js"></script>
  <script type="text/javascript"><!--
    $("#titleContainer").vortexTips("abbr", "#titleContainer", 200, 300, 250, 300, 20, 0);
  // -->
  </script>
  <p>
    <abbr title="<h4>${filesTipI18n}</h4><@vrtx.fileNamesAsLimitedList session.filesToBeCopied />">
      ${session.filesToBeCopied?size} ${filesI18n}
    </abbr>
  </p>
</#if>
<form id="vrtx-move-to-selected-folder" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
    <div class="vrtx-button-small"><button title="${titleMsg}" type="submit"
            id="vrtx-move-to-selected-folder.submit"
            value="move-resources-to-this-folder" name="action">${item.title?html}</button></div>
</form>

<#recover>
${.error}
</#recover>