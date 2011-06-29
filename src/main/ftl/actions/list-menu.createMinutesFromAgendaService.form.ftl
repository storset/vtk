<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if command?exists && !command.done>
  <div class="globalmenu">
  <form name="form" action="${command.submitURL?html}" method="POST">
    <h3 class="nonul"><@vrtx.msg code="actions.createMinutes"
    default="Make minutes"/>:</h3>
    <@spring.bind "command.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <div class="vrtx-textfield">
      <input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}">
    </div>
    <div id="submitButtons">
      <div class="vrtx-focus-button">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.createMinutes.save" default="Create"/>">
      </div>
      <div class="vrtx-button">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createMinutes.cancel" default="Cancel"/>">
      </div>
    </div>
  </form>
  <script type="text/javascript">
  <!--          
  document.form.name.focus();
  // -->
  </script>
  </div>
  </#if>
<#recover>
${.error}
</#recover>
