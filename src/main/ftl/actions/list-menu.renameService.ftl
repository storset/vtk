<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.renameService.title") />
<#assign actionURL = item.url />

<a id="renameService" title="${titleMsg}" href="${actionURL?html}">${item.title?html}</a>

<#recover>
${.error}
</#recover>