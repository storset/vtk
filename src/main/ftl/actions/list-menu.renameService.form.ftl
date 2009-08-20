<#attempt>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if renameForm?exists && !renameForm.done>
    <form name="rename" class="globalmenu" action="${renameForm.submitURL?html}" method="post">
      <h3><@vrtx.msg code="actions.renameService" default="Change name"/>:</h3>
      <@spring.bind "renameForm" + ".name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
      <input type="text" size="20" name="name" value="${renameForm.name}">
      <div id="submitButtons">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.renameService.save" default="Save"/>">
        <input type="submit" name="cancel" value="<@vrtx.msg code="actions.renameService.cancel" default="Cancel"/>"/>
      </div>
    </form>
  </#if>
<#recover>
${.error}
</#recover>


