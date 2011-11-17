<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#if type = "publish.globalPublishResourceConfirmedService">
<div class="globalmenu expandedForm">
<form name="vrtx-publish-document" id="vrtx-publish-document-form" action="${url}" method="post">
  <h3>${vrtx.getMsg("confirm-publish.confirmation.publish")}?</h3>
  <div class="submitButtons">
    <div class="vrtx-focus-button">
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
