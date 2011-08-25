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
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</title>
</head>


<#if resourceContext.currentResource.lock?exists>
  <#assign owner = resourceContext.currentResource.lock.principal.qualifiedName />
</#if>
<#assign currentPrincipal = resourceContext.principal.qualifiedName />

<#if !owner?exists || owner = currentPrincipal >
  <body onload="document.unlockForm.unlock.click()">
<#else>
  <body>
</#if>
  <h1>${vrtx.getMsg("unlockwarning.title")} '${resourceContext.currentResource.name}'</h1>
  <form method="post" action="${form.url?html}" name="unlockForm">
    <@vrtx.csrfPreventionToken url=form.url />
    <#if owner?exists && owner != currentPrincipal>
      <p>${vrtx.getMsg("unlockwarning.steal")}: <strong>${owner}</strong>.</p> 
      <p>${vrtx.getMsg("unlockwarning.modified")}: <strong>${resourceContext.currentResource.lastModified?datetime?html}</strong>.</p>
      <p>${vrtx.getMsg("unlockwarning.explanation")}</p>
    </#if>
    <div id="vrtx-unlock-buttons" class="submitButtons">
      <div class="vrtx-focus-button">
        <button tabindex="1" type="submit" name="unlock" value="Unlock" >
          ${vrtx.getMsg("unlockwarning.unlock")}
        </button>
      </div>
      <div class="vrtx-button">
        <button tabindex="2" type="submit" name="cancel" id="cancel" value="Cancel" >
          ${vrtx.getMsg("unlockwarning.cancel")}
        </button>
      </div>
    </div>
  </form>
</body>
</html>
 