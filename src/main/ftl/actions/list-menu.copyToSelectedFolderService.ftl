<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("copyMove.copy.title") />
<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL + '&showAsHtml=true&height=80&width=230' />
  <#assign method = "get" />
</#if>
<form id="vrtx-copy-to-selected-folder" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
  <button title="${titleMsg}" type="submit"
          id="vrtx-copy-to-selected-folder.submit"
          value="copy-resources-to-this-folder" name="action">${item.title?html}</button>
</form>
<#recover>
${.error}
</#recover>
