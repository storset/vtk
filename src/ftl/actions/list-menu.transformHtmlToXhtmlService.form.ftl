<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if command?exists && !command.done>

  <#-- Need this div coz of IEs sucky boxmodel implementation -->
  <!-- div style="clear:both;"></div -->
  <form name="form" class="action-bar collectionMenu" action="${command.submitURL?html}" method="POST">
    <h3 class="nonul"><@vrtx.msg code="lcms.publish.title"
    default="Make webeditable copy"/>:</h3>
    <@spring.bind "command.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <div><input type="text" name="${spring.status.expression}" value="${spring.status.value?if_exists}"></div>
    <input type="submit" name="save" value="<@vrtx.msg code="actions.createCollectionService.save" default="Create"/>">
    <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createCollectionService.cancel" default="Cancel"/>">
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
