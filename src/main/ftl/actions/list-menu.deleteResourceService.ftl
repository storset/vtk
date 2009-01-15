<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

(&nbsp;<a href="${item.url?html}&showAsHtml=true&height=80&width=230" class="thickbox">${item.title}</a>&nbsp;)

<#recover>
${.error}
</#recover>
