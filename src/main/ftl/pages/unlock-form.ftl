<#--
  - File: unlock-form.ftl
  - 
  - Description: Display (and if possible, autosubmit unlock form)
  - 
  - Required model data:
  -   form
  -   resourceContext
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<title>Unlock resource '${resourceContext.currentResource.name}'</title>
</head>
<#assign owner = resourceContext.currentResource.lock.principal.qualifiedName />
<#assign currentPrincipal = resourceContext.principal.qualifiedName />
<#if owner = currentPrincipal>
<body onload="document.forms[0].submit()">
<#else>
<body>
</#if>
  <h1>Unlock resource '${resourceContext.currentResource.name}'</h1>

  <form method="post" action="${form.url?html}">
    <@vrtx.csrfPreventionToken url=form.url />
    <p>TODO: fix this message</p>
    <#if owner != currentPrincipal>
      You are about to steal a lock from user ${owner}. Resource was last
    modified on ${resourceContext.currentResource.lastModified?datetime?html}.
    <#else>
    </#if>
    <input type="submit" name="unlock" value="Unlock" />
    <input type="submit" name="cancel" value="Cancel" />
  </form>
</body>
</html>
