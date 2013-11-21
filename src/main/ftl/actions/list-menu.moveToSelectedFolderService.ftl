<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("copyMove.move-resources.header") />
<#assign titleMsg = vrtx.getMsg("copyMove.move.title") />
<#assign clearTitleMsg = vrtx.getMsg("copyMove.move.clear.title") />
<#assign filesI18n = vrtx.getMsg("copyMove.files") />
<#assign filesTipI18n = vrtx.getMsg("copyMove.files.move.tip.title") /> 
<#assign actionURL = item.url />
<#assign method = "post" />
<#if resourcesDisclosed?exists>
  <#assign actionURL =  warningDialogURL />
  <#assign method = "get" />
</#if>

<h3>${headerMsg}</h3>
<#if session.filesToBeCopied?exists>
  <p>
    <abbr title="<h4 id='title-wrapper'>${filesTipI18n}</h4><@vrtx.fileNamesAsLimitedList session.filesToBeCopied />">
      ${session.filesToBeCopied?size} ${filesI18n}
    </abbr>
  </p>
</#if>

<#if !resourcesDisclosed?exists>
  <form id="vrtx-move-to-selected-folder" action="${actionURL?html}" method="${method}" class="vrtx-admin-button">
     <div class="vrtx-button-small first"><button title="${titleMsg}" type="submit"
          id="vrtx-copy-to-selected-folder.submit"
          value="copy-resources-to-this-folder" name="action">${item.title?html}</button></div>
     <div class="vrtx-button-small last"><button title="${clearTitleMsg}" type="submit"
          id="vrtx-copy-to-selected-folder.clear"
          value="clear-action" name="clear-action">x</button></div>
  </form>
<#else>
  <a class="vrtx-button-small vrtx-copy-move-to-selected-folder-disclosed" title="${titleMsg}" id="vrtx-move-to-selected-folder" href="${actionURL?html}"><span>${item.title?html}</span></a>
</#if>

<#recover>
${.error}
</#recover>
