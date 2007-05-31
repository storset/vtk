<#ftl strip_whitespace=true>

<#--
  - File: repository-read-only.ftl
  - 
  - Description: A HTML page that gives access to switch read only status
  - 
  - Required model data:
  - message
  -  
  - Optional model data:
  - repository.readOnlySwitchUrl 
  -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Read only status</title>
  </head>
  <body>
    <h1>Read only status</h1>
    <p><#if message?exists>
          Repository is in read only mode
          <#if unsetReadOnlyUrl?exists>
            (<a href="${unsetReadOnlyUrl.url?html}">switch</a>)
          </#if>
       <#else> 
          Repository is not in read only mode
          <#if setReadOnlyUrl?exists>
            (<a href="${setReadOnlyUrl.url?html}">switch</a>)
          </#if>
       </#if>
    </p>
  </body>
</html>
