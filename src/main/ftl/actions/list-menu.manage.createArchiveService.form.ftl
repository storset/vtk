<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if command?exists && !command.done>
  <div class="globalmenu">
  <form name="form" action="${command.submitURL?html}" method="post">
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
    <div class="vrtx-textfield">
      <input type="text" size="30" name="${spring.status.expression}" value="${spring.status.value?if_exists}">
    </div>
    
    <p>
    <div>Enter comma separated list of paths to ignore. Paths <b>MUST</b> be entered as they are written in the manifest, i.e. all 
    start with a slash ("/") and collections also end with one.<br/>
    <div class="vrtx-textfield">
      <input type="text" size="30" name="ignorableResources" id="ignorableResources" value=""></div>
    </div>
    <div style="font-size: 0.769em">Disclaimer: If you don't know what the contents of this field does, then for the love of God don't put anything in it.</div>
    </p>
    
    <div id="submitButtons">
      <div class="vrtx-focus-button">
        <input type="submit" name="save" value="<@vrtx.msg code="actions.createArchive.save" default="Create"/>">
      </div>
      <div class="vrtx-button">
        <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createArchive.cancel" default="Cancel"/>">
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
