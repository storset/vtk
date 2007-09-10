<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.copyUnCheckedMessage",
         "You must check at least one element to copy") />

${prepend}<a href="javascript:copyMoveAction('${item.url?html}', '${unCheckedMessage}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
