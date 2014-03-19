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
    <abbr tabindex="0" title="<h4 id='title-wrapper'>${filesTipI18n}</h4><@vrtx.fileNamesAsLimitedList session.filesToBeCopied />">
      ${session.filesToBeCopied?size} ${filesI18n}
    </abbr>
  </p>
</#if>

<#if !resourcesDisclosed?exists>
  <#if moveToSameFolder?has_content>
    <span id="move-to-same-folder">yes</span>
  </#if>
  <#if existingFilenames?has_content>
    <span id="copy-move-existing-filenames"><#list existingFilenames as filename>${filename?html}<#if filename_has_next>#</#if></#list></span>
    <span id="copy-move-number-of-files">${session.filesToBeCopied?size}</span>
  </#if>
  <form id="vrtx-move-to-selected-folder" action="${actionURL?html}" method="${method}">
     <button class="vrtx-button-small first" title="${titleMsg}" type="submit" value="copy-resources-to-this-folder" name="action">${item.title?html}</button>
     <button class="vrtx-cancel-link" title="${clearTitleMsg}" type="submit" value="clear-action" name="clear-action">x</button>
  </form>
<#else>
  <form id="vrtx-move-to-selected-folder" action="${item.url?html}" method="post">
    <a class="vrtx-button-small vrtx-copy-move-to-selected-folder-disclosed" title="${titleMsg}" id="vrtx-move-to-selected-folder" href="${actionURL?html}">${item.title?html}</a>
    <button class="vrtx-cancel-link" title="${clearTitleMsg}" type="submit" value="clear-action" name="clear-action">x</button>
  </form>
</#if>

<#recover>
${.error}
</#recover>
