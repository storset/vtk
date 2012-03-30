<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/actions.ftl" as actionsLib />

<#if command?exists && !command.done>
  <div class="globalmenu expandedForm">
    <form name="form" action="${command.submitURL?html}" method="POST">
      <h3 class="nonul"><@vrtx.msg code="actions.transformHtmlToXhtmlService"
        default="Make webeditable copy"/>:</h3>
      <@spring.bind "command.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	    </ul>
      </#if>
      <div class="vrtx-textfield">
        <input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}" />
      </div>
      <@actionsLib.genOkCancelButtons "save" "cancelAction" "actions.transformHtmlToXhtmlService.save" "actions.transformHtmlToXhtmlService.cancel" />
    </form>
  </div>
</#if>
  
<#recover>
${.error}
</#recover>
