<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if command?exists && !command.done>
  <div class="globalmenu expandedForm">
    <form name="form" action="${command.submitURL?html}" method="POST">
      <h3 class="nonul"><@vrtx.msg code="actions.createMinutes" default="Make minutes"/>:</h3>
      <@spring.bind "command.name" /> 
      <@actionsLib.genOkCancelButtons spring.status.errorMessages />
      <div class="vrtx-textfield">
        <input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}" />
      </div>
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createMinutes.save" "actions.createMinutes.cancel" />
    </form>
  </div>
</#if>
<#recover>
${.error}
</#recover>
