<#attempt>
<form name="vrtx-delete-resource" class="globalmenu" id="vrtx-delete-resource" action="${url}" method="post">
${vrtx.getMsg("collectionListing.confirmation.delete")} <span class="vrtx-confirm-delete-name"> ${name}</span>? 
  <button tabindex="1" type="submit" value="ok" id="deleteResourceAction" name="deleteResourceAction">
    ${vrtx.getMsg("confirm-delete.ok")}
  </button>
  <button tabindex="2" type="submit" value="cancel" id="deleteResourceCancelAction" name="deleteResourceCancelAction">
    ${vrtx.getMsg("confirm-delete.cancel")}
  </button>
</form>
<#recover>
${.error}
</#recover>
