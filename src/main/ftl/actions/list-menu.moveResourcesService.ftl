<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.moveUnCheckedMessage",
         "You must check at least one element to move") />
                 
${prepend}<a id="moveResourceService" href="javascript:copyMoveAction('${item.url?url('ISO-8859-1')}', '${unCheckedMessage}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
