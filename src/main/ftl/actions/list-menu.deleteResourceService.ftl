<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<a id="deleteResourceService" href="${item.url?html}">${item.title}</a>

<#recover>
${.error}
</#recover>
