<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("copyMove.move-resources.header") />
<#assign titleMsg = vrtx.getMsg("copyMove.move.title") />
<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=110&width=250' />
  <#assign method = "get" />
</#if>

<h3>${headerMsg}</h3>
<#if session?exists && session.filesToBeCopied?exists>
  <p>${session.filesToBeCopied?size} filer</p>
</#if>
<form id="vrtx-move-to-selected-folder" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
    <div class="vrtx-button-small"><button title="${titleMsg}" type="submit"
            id="vrtx-move-to-selected-folder.submit"
            value="move-resources-to-this-folder" name="action">${item.title?html}</button></div>
</form>

<#recover>
${.error}
</#recover>
