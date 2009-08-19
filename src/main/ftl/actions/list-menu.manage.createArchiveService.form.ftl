<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if command?exists && !command.done>

  <form name="form" class="globalmenu" action="${command.submitURL?html}" method="post">
    <h3 class="nonul"><@vrtx.msg code="actions.createArchive"
    default="Create archive"/>:</h3>
    <@spring.bind "command.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <div><input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}"></div>
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.createArchive.save" default="Create"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createArchive.cancel" default="Cancel"/>">
    </div>
  </form>
  <script language="JavaScript" type="text/javascript">
  <!--          
  document.form.name.focus();
  // -->
  </script>

  </#if>
<#recover>
${.error}
</#recover>
