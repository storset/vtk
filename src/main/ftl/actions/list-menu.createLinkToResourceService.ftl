<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#if editField?exists && browseUrl?exists>
${prepend}<a href="javascript:updateParent('${editField}', '${browseURL}')">${item.title}</a>${append}
</#if>

<#recover>
${.error}
</#recover>
