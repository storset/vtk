<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext.currentResource.isCollection()>
  <#assign titleMsg = vrtx.getMsg("confirm-delete.title.folder") />
<#else>
  <#assign titleMsg = vrtx.getMsg("confirm-delete.title.file") />
</#if>
(&nbsp;<a href="${item.url?html}" title="${titleMsg}">${item.title}</a>&nbsp;)

<#recover>
${.error}
</#recover>
