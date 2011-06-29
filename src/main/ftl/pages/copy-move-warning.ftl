<#ftl strip_whitespace=true>

<#--
  - File: copy-move-warning.ftl
  - 
  - Description: Displays a confirmation dialog when copying or moving
  - resources into a location where they are made readable for everyone
  - 
  - Required model data:
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<#if action = "move-resources">
<title>${vrtx.getMsg("copyMove.move.title")}</title>
<#else>
<title>${vrtx.getMsg("copyMove.copy.title")}</title>
</#if>
</head>
<body>

<div class="vrtx-confirm-copy-move-explanation">
  <#if action = "move-resources">
    ${vrtx.getMsg("copyMove.move.discloseWarning.explanation")}
  <#else>
    ${vrtx.getMsg("copyMove.copy.discloseWarning.explanation")}
  </#if>
</div>   
<div class="vrtx-confirm-copy-move-confirmation">
  <#if action = "move-resources">
    ${vrtx.getMsg("copyMove.move.discloseWarning.confirm")}
  <#else>
    ${vrtx.getMsg("copyMove.copy.discloseWarning.confirm")}
  </#if>
</div>

<#if action = "move-resources"><#assign formURL = move.url?html /><#else><#assign formURL = copy.url?html /></#if>

<form name="vrtx-confirm-copy-move" id="vrtx-confirm-copy-move" action="${formURL}" method="post">
  <div class="submitButtons">
    <div class="vrtx-focus-button">
      <button tabindex="1" type="submit" value="ok" id="confirmAction" name="confirm-action">
        ${vrtx.getMsg("copyMove.discloseWarning.ok")}
      </button>
    </div>
    <div class="vrtx-button">
      <button tabindex="2" type="submit" value="cancel" id="cancelAction" name="cancel-action">
        ${vrtx.getMsg("copyMove.discloseWarning.cancel")}
      </button>
    </div>
  </div>
</form>

<script type="text/javascript"><!--
	function f_focus(){
		$("#confirmAction").focus();
	}
	
	$(document).ready(function(){
	   setTimeout("focus();",0);
	   $("#cancelAction").remove(); 
	   $("#vrtx-confirm-copy-move").append('<button tabindex="2" type="button" onclick="tb_remove();" id="cancelAction" name="cancelAction">${vrtx.getMsg("copyMove.discloseWarning.cancel")}</button>');
	});
        //-->
</script>

</body>
</html>
