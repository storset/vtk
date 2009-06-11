<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("copyMove.move.title") />
<#if resourcesDisclosed?exists>
(&nbsp;<a href="${warningDialogURL?html}&amp;showAsHtml=true&amp;height=100&amp;width=250"
          class="thickbox" title="${titleMsg}">${item.title}</a>&nbsp;)
<#else>
(&nbsp;<a href="${item.url?html}" title="${titleMsg}">${item.title}</a>&nbsp;)
</#if>

<#recover>
${.error}
</#recover>
