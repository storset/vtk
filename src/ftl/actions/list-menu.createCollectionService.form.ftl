<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createCollectionForm?exists && !createCollectionForm.done>
  <#-- Need this div coz of IEs sucky boxmodel implementation -->
  <div style="clear:both;height:1px;visibility:hidden;"></div>
  <form name="createcollection" action="${createCollectionForm.submitURL?html}" method="POST">
    <h3 class="nonul"><@vrtx.msg code="actions.createCollectionService" default="Collection Name"/>:</h3>
    <@spring.bind "createCollectionForm.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <input type="text" name="name">
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.createCollectionService.save" default="Create"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createCollectionService.cancel" default="Cancel"/>">
    </div>
  </form>
  <script language="JavaScript" type="text/javascript">
  <!--          
  document.createcollection.name.focus();
  // -->
  </script>
  </#if>

<#recover>
${.error}
</#recover>
