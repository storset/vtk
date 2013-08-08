<#ftl strip_whitespace=true>
<#--
  - File: actions.ftl
  - 
  - Description: Library for actions
  -   
  -->
<#import "vortikal.ftl" as vrtx />

<#macro genOkCancelButtons nameOk nameCancel msgOk msgCancel>
  <div id="submitButtons">
    <div class="vrtx-focus-button">
      <input type="submit" name="${nameOk}" value="<@vrtx.msg code="${msgOk}" default="Ok"/>" />
    </div>
    <div class="vrtx-button">
      <input type="submit" name="${nameCancel}" value="<@vrtx.msg code="${msgCancel}" default="Cancel"/>" />
    </div>
  </div>
</#macro>

<#macro genErrorMessages errors>
  <#if (errors?size > 0)>
    <div class="errorContainer">
      <ul class="errors">
        <#list errors as error> 
          <li>${error}</li> 
        </#list>
	  </ul>
    </div>
  </#if>
</#macro>

<#macro copyMove item parentOp>
  <#if resourceContext.parentURI = "/">
    <#if resourceContext.currentResource.collection>
      <#assign actionURL = vrtx.linkConstructor('../', 'manageService') />
    <#else>
      <#assign actionURL = vrtx.linkConstructor('./', 'manageService') />
    </#if>
  <#else>
    <#assign actionURL = vrtx.linkConstructor(resourceContext.parentURI, 'manageService') />
  </#if>

  <form method="post" action="${actionURL?html}" name="${parentOp}-single-form">
    <@vrtx.csrfPreventionToken url=actionURL />
    <input type="hidden" name="action" value="${parentOp}"  />
    <input type="hidden" name="${resourceContext.currentURI}" />
    <input type="submit" value="${item.title}" />
  </form>
</#macro>