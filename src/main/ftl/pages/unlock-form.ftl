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
<title>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</title>
</head>
<#assign owner = resourceContext.currentResource.lock.principal.qualifiedName />
<#assign currentPrincipal = resourceContext.principal.qualifiedName />
<#if owner = currentPrincipal>
<body onload="document.forms[0].submit()">
<#else>
<body>
</#if>
  <h1>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</h1>
  <form method="post" action="${form.url?html}">
    <@vrtx.csrfPreventionToken url=form.url />
    <#if owner != currentPrincipal>
    <p>${vrtx.getMsg("unlockwarning.steal")} ${owner}.</p> 
    <p>${vrtx.getMsg("unlockwarning.modified")} ${resourceContext.currentResource.lastModified?datetime?html}.</p>
    <p>${vrtx.getMsg("unlockwarning.explanation")}</p>
    <#else>
    </#if>
    <button tabindex="1" type="submit" name="unlock" value="Unlock" >
      ${vrtx.getMsg("unlockwarning.unlock")}
    </button>
    <button tabindex="2" type="submit" name="cancel" value="Cancel" >
      ${vrtx.getMsg("unlockwarning.cancel")}
    </button>
  </form>
</body>
</html>
 