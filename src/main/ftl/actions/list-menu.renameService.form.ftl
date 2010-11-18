<#attempt>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if renameCommand?exists>
    <form name="rename" class="globalmenu" action="${renameCommand.submitURL?html}" method="post">
      <h3><@vrtx.msg code="actions.renameService" default="Change name"/>:</h3>
      <@spring.bind "renameCommand" + ".name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
		</ul>
      </#if>
      <#assign confirm = renameCommand.confirmOverwrite />
      <input type="text" size="20" name="name" value="${spring.status.value}" <#if confirm> readonly="readonly" </#if> />
      <div id="submitButtons">
      	<#if confirm>
      		<input type="submit" name="overwrite" value="<@vrtx.msg code="actions.renameService.overwrite" default="Overwrite"/>">
      	<#else>
        	<input type="submit" name="save" value="<@vrtx.msg code="actions.renameService.save" default="Save"/>">
        </#if>
        	<input type="submit" name="cancel" value="<@vrtx.msg code="actions.renameService.cancel" default="Cancel"/>"/>
      </div>
    </form>
  </#if>
<#recover>
${.error}
</#recover>


