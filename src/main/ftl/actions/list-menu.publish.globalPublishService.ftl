<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign headerMsg = vrtx.getMsg("publish.header") />
<#assign titleMsg = vrtx.getMsg("publish.title") />

<#assign actionURL = item.url />

<h3>${headerMsg}</h3>
<p><span class="unpublished"><@vrtx.msg code="publishing.unpublish-date" /></span></p>
<a id="vrtx-publish-document" class="vrtx-button-small" title="${titleMsg}" href="${actionURL?html}"><span>${item.title?html}</span></a>

<#recover>
${.error}
</#recover>
