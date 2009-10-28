<#ftl strip_whitespace=true>

<#--
  - File: confirm-delete.ftl
  - 
  - Description: Displays a publish confirmation dialog
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
<div class="vrtx-confirm-publish-msg">
${vrtx.getMsg("confirm-publish.confirmation.publish")} <span class="vrtx-confirm-publish-name"> ${name}</span>? 
</div>   

<form name="vrtx-publish-resource" id="vrtx-publish-resource" action="${url}" method="post">
  <button tabindex="1" type="submit" value="ok" id="publishResourceAction" name="publishResourceAction">
    ${vrtx.getMsg("confirm-delete.ok")}
  </button>
  <button tabindex="2" type="submit" value="cancel" id="publishResourceCancelAction" name="publishResourceCancelAction">
    ${vrtx.getMsg("confirm-delete.cancel")}
  </button>
</form>

<script language="javascript"><!--
   function focus(){
      $("#publishResourceAction").focus();
   }
	
   $(document).ready(function(){
      setTimeout("focus();",0);
      $("#publishResourceCancelAction").remove(); 
      $("#vrtx-publish-resource").append('<button tabindex="2" type="button" onclick="tb_remove();" id="publishResourceCancelAction" name="publishResourceCancelAction">${vrtx.getMsg("confirm-delete.cancel")}</button>');
   });
// -->
</script>
</body>
</html>
