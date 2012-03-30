<#ftl strip_whitespace=true>
<#attempt>
<#import "/lib/vortikal.ftl" as vrtx />

<div class="globalmenu expandedForm">
  <form name="vrtx-delete-resource" id="vrtx-delete-resource" action="${url}" method="post">
    ${vrtx.getMsg("collectionListing.confirmation.delete")} <span class="vrtx-confirm-delete-name"> ${name}</span>? 
    <@actionsLib.genOkCancelButtons "deleteResourceAction" "deleteResourceCancelAction" "confirm-delete.ok" "confirm-delete.cancel" />
  </form>
</div>

<#recover>
${.error}
</#recover>
