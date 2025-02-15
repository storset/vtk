<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vtk.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if command?exists && !command.done>
  <div class="globalmenu expandedForm">
    <form name="form" action="${command.submitURL?html}" method="post">
      <h3 class="nonul"><@vrtx.msg code="actions.createMinutes" default="Make minutes"/>:</h3>
      <@spring.bind "command.name" /> 
      <@actionsLib.genErrorMessages spring.status.errorMessages />
      <input class="vrtx-textfield" type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}" />
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.createMinutes.save" "actions.createMinutes.cancel" />
    </form>
  </div>
</#if>

<#recover>
${.error}
</#recover>
