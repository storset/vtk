<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if command?exists && !command.done>

  <form name="form" class="globalmenu" action="${command.submitURL?html}" method="POST">
    <h3 class="nonul"><@vrtx.msg code="actions.expandArchive"
    default="Expand archive"/>:</h3>
    <@spring.bind "command.name" /> 
    <#if spring.status.errorMessages?size &gt; 0>
      <ul class="errors">
      <#list spring.status.errorMessages as error> 
        <li>${error}</li> 
      </#list>
	  </ul>
    </#if>
    <div><input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}"></div>
    
    <p>
    <div>Enter comma separated list of paths to ignore. Paths <b>MUST</b> be entered as they are written in the manifest, i.e. all 
    start with a slash ("/") and collections also end with one.<br/>
    <input type="text" size="30" name="ignorableResources" id="ignorableResources" value=""></div>
    <div style="font-size: 6px">Disclaimer: If you don't know what the contents of this field does, then for the love of God don't put anything in it.</div>
    </p>
    
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.expandArchive.save" default="Expand"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.expandArchive.cancel" default="Cancel"/>">
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
