<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

${prepend}<a href="javascript:copyMoveAction('${item.url?html}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
