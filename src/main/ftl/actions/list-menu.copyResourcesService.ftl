<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.copyUnCheckedMessage",
         "You must check at least one element to copy") />

${prepend}<a id="copyResourceService" href="javascript:copyMoveAction('${item.url?url('ISO-8859-1')}', '${unCheckedMessage}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
