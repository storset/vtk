<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign titleMsg = vrtx.getMsg("actions.expandArchive.title") />
<#assign actionURL = item.url />

<a id="manage.expandArchiveService" title="${titleMsg}" href="${actionURL?html}">${item.title?html}</a>

<#recover>
${.error}
</#recover>