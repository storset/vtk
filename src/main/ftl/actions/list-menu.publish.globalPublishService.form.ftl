<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#if type = "publish.publishResourceConfirmedService">
<div class="globalmenu">
<form name="vrtx-publish-resource" id="vrtx-publish-resource" action="${url}" method="post">
  <h3>${vrtx.getMsg("confirm-publish.confirmation.publish")}?</h3>
  <div class="submitButtons">
    <div class="vrtx-button">
      <button tabindex="1" type="submit" value="ok" id="publishResourceAction" name="publishResourceAction">
        ${vrtx.getMsg("confirm-delete.ok")}
      </button>
    </div>
    <div class="vrtx-button">
      <button tabindex="2" type="submit" value="cancel" id="publishResourceCancelAction" name="publishResourceCancelAction">
        ${vrtx.getMsg("confirm-delete.cancel")}
      </button>
    </div>
  </div>
</form>
</div>
</#if>

<#recover>
${.error}
</#recover>
