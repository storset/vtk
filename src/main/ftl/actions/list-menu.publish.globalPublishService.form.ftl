<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#if type = "publish.publishResourceConfirmedService">
<form name="vrtx-publish-resource" class="globalmenu" id="vrtx-publish-resource" action="${url}" method="post">
  <h3>${vrtx.getMsg("confirm-publish.confirmation.publish")}?</h3>
  <button tabindex="1" type="submit" value="ok" id="publishResourceAction" name="publishResourceAction">
    ${vrtx.getMsg("confirm-delete.ok")}
  </button>
  <button tabindex="2" type="submit" value="cancel" id="publishResourceCancelAction" name="publishResourceCancelAction">
    ${vrtx.getMsg("confirm-delete.cancel")}
  </button>
</form>
</#if>

<#recover>
${.error}
</#recover>
