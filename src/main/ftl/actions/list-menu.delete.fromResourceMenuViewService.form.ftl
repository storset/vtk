<#attempt>
<div class="globalmenu expandedForm">
  <form name="vrtx-delete-resource" id="vrtx-delete-resource" action="${url}" method="post">
    ${vrtx.getMsg("collectionListing.confirmation.delete")} <span class="vrtx-confirm-delete-name"> ${name}</span>? 
    <div class="submitButtons">
      <div class="vrtx-focus-button">
        <button tabindex="1" type="submit" value="ok" id="deleteResourceAction" name="deleteResourceAction">
          ${vrtx.getMsg("confirm-delete.ok")}
        </button>
      </div>
      <div class="vrtx-button">
        <button tabindex="2" type="submit" value="cancel" id="deleteResourceCancelAction" name="deleteResourceCancelAction">
          ${vrtx.getMsg("confirm-delete.cancel")}
        </button>
      </div>
    </div>
  </form>
</div>

<#recover>
${.error}
</#recover>
