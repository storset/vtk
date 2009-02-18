<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.moveUnCheckedMessage",
         "You must check at least one element to move") />
        
<#if createErrorMessage?exists >
   <#assign moveError = vrtx.getMsg("${createErrorMessage}", "One or more of the resources already exists in the destination-path.") />
</#if>

     <#if moveError?exists >
          <ul class="errors">
               <li>${moveError}</li> 
          </ul>
     </#if>

${prepend}<a href="javascript:copyMoveAction('${item.url?url('ISO-8859-1')}', '${unCheckedMessage}')">${item.title}</a>${append}

<#recover>
${.error}
</#recover>
