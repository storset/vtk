<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("copyMove.move.title") />
<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=80&width=230' />
  <#assign method = "get" />
</#if>

<form id="vrtx-move-to-selected-folder" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
  <button title="${titleMsg}" type="submit"
          id="vrtx-move-to-selected-folder.submit"
          value="move-resources-to-this-folder" name="action">${item.title?html}</button>
</form>
<#recover>
${.error}
</#recover>
