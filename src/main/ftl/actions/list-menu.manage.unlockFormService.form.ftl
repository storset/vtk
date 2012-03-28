<#ftl strip_whitespace=true>
<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<div class="globalmenu expandedForm">
  <form id="manage.unlockFormService-form" method="post" action="${unlockFormCommand.submitURL?html}" name="unlockForm">
    <h3><@vrtx.msg code="resourceMenuRight.manage.unlockFormService" default="Unlock"/></h3>
    <#if resourceContext.currentResource.lock?exists>
      <#assign owner = resourceContext.currentResource.lock.principal.qualifiedName />
    </#if>
    <#if owner?exists>
      <p>${vrtx.getMsg("unlockwarning.steal")}: <strong>${owner}</strong>.</p> 
      <p>${vrtx.getMsg("unlockwarning.modified")}: <strong>${resourceContext.currentResource.lastModified?datetime?html}</strong>.</p>
      <p>${vrtx.getMsg("unlockwarning.explanation")}</p>
    </#if>
    <div class="submitButtons">
      <div class="vrtx-focus-button">
        <button tabindex="1" type="submit" name="unlock">
          ${vrtx.getMsg("unlockwarning.unlock")}
        </button>
      </div>
      <div class="vrtx-button">
        <button tabindex="2" type="submit" name="cancel" >
          ${vrtx.getMsg("unlockwarning.cancel")}
        </button>
      </div>
    </div> 
  </form>
</div>
<#recover>
${.error}
</#recover>