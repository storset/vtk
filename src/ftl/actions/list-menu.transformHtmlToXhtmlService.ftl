<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign msg = vrtx.getMsg("manage.create.resource.infoMessage", "A new webeditable document will be created based on this document ending with -webredigerbar.html") />
${prepend}<a href="${item.url?html}" onclick="return alert('${msg}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>


