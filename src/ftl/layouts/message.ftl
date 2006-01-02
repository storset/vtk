<#--
  - File: message.ftl
  - 
  - Description: (Error) messages in manage header
  - 
  - Optional model data:
  -   createErrorMessage
  -   message
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />

  <#if createErrorMessage?exists>
     <p class="errormessage"><@vrtx.msg code="manage.create.${createErrorMessage}" default="${createErrorMessage}"/></p>
  </#if>

  <#if infoMessage?exists>
    <div class="infomessage"><@vrtx.msg code="${infoMessage}" default="${infoMessage}"/></div>
  </#if>

  <#if message?exists>
    <p class="errormessage"><@vrtx.msg code="${message}" default="${message}"/></p>
  </#if>

  <#if errorItems?exists>
    <ul class="errors">
    <#list errorItems as item>
      <li>${item}</li>
    </#list>
    </ul>
  </#if>
