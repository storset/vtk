<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if resourceContext.currentResource.isCollection()>
	<#assign titleMsg = vrtx.getMsg("confirm-delete.title") + " " + vrtx.getMsg("confirm-delete.folder")  />
<#else>
	 <#assign titleMsg = vrtx.getMsg("confirm-delete.title") + " " + vrtx.getMsg("confirm-delete.file")  />
</#if>

(&nbsp;<a href="${item.url?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" class="thickbox" title="${titleMsg}">${item.title}</a>&nbsp;)

<#recover>
${.error}
</#recover>
