<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext.currentResource.isCollection()>
  <#assign titleMsg = vrtx.getMsg("confirm-delete.title.folder") />
<#else>
  <#assign titleMsg = vrtx.getMsg("confirm-delete.title.file") />
</#if>
<a class="vrtx-button" href="${item.url?html}" title="${titleMsg}"><span>${item.title}</span></a>

<#recover>
${.error}
</#recover>
