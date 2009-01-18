<#ftl strip_whitespace=true>

<#--
  - File: tags.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -   tag
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<body>
<div class="vrtx-confirm-delete-msg">
${vrtx.getMsg("collectionListing.confirmation.delete")} <div class="vrtx-confirm-delete-name"> ${name}</div>? 
</div>   

<form name="vrtx-delete-resource" action="${url}" method="post">
	<input type="hidden" value="delete" name="action" id="action" />
	<button tabindex="0" type="submit" value="ok" id="vrtx-delete" name="vrtx-delete">${vrtx.getMsg("confirm-delete.ok")}</button>
    <button tabindex="1" type="submit" value="cancel" id="vrtx-delete-cancel" name="vrtx-delete-cancel">${vrtx.getMsg("confirm-delete.cancel")}</button>
</form>

</body>
</html>
