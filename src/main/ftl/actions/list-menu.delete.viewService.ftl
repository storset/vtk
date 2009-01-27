<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext.currentResource.isCollection()>
	<#assign titleMsg = vrtx.getMsg("confirm-delete.title") + " " + vrtx.getMsg("confirm-delete.folder")  />
<#else>
	 <#assign titleMsg = vrtx.getMsg("confirm-delete.title") + " " + vrtx.getMsg("confirm-delete.file")  />
</#if>
(&nbsp;<a id="${item.label}" href="${item.url?html}&showAsHtml=true&height=80&width=230" class="thickbox" title="${titleMsg}">${item.title}</a>&nbsp;)
<#recover>
${.error}
</#recover>
