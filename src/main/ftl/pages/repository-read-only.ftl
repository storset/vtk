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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Read only status</title>
  </head>
  <body>
    <h2>Read only status for ${resourceContext.repositoryId?html}</h2>
    <p><#if resourceContext.repositoryReadOnly>
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
