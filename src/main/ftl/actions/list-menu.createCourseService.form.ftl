<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createCourseForm?exists && !createCourseForm.done>
  <form name="createcourse" class="globalmenu" action="${createCourseForm.submitURL?html}" method="post">
    <h3><@vrtx.msg code="actions.createCourseService" default="Coursecode"/>:</h3>
    <@spring.bind "createCourseForm.name" /> 
      <#if spring.status.errorMessages?size &gt; 0>
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
	</ul>
      </#if>
    <input type="text" name="name">
    <div id="submitButtons">
      <input type="submit" name="save" value="<@vrtx.msg code="actions.createCourseService.save" default="Create"/>">
      <input type="submit" name="cancelAction" value="<@vrtx.msg code="actions.createCourseService.cancel" default="Cancel"/>">
    </div>
  </form>
  <script language="JavaScript" type="text/javascript">
  <!--          
  document.createcourse.name.focus();
  // -->
  </script>
  </#if>

<#recover>
${.error}
</#recover>
