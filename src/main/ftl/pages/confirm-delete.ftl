<#ftl strip_whitespace=true>

<#--
  - File: confirm-delete.ftl
  - 
  - Description: Displays a delete confirmation dialog
  - 
  - Required model data:
  -   url
  -   name
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<body>
<div class="vrtx-confirm-delete-msg">
${vrtx.getMsg("collectionListing.confirmation.delete")} <span class="vrtx-confirm-delete-name"> ${name}</span>? 
</div>   

<form name="vrtx-delete-resource" id="vrtx-delete-resource" action="${url}" method="post">
  <div class="submitButtons">
    <div class="vrtx-button">
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

<script type="text/javascript"><!--
   function focus(){
      $("#deleteResourceAction").focus();
   }
	
   $(document).ready(function(){
      setTimeout("focus();",0);
      $("#deleteResourceCancelAction").remove(); 
      $("#vrtx-delete-resource").append('<button tabindex="2" type="button" onclick="tb_remove();" id="deleteResourceCancelAction" name="deleteResourceCancelAction">${vrtx.getMsg("confirm-delete.cancel")}</button>');
   });
// -->
</script>
</body>
</html>
