<#ftl strip_whitespace=true>

<#--
  - File: confirm-publish.ftl
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
<#if type = "publish.unpublishResourceConfirmedService">
${vrtx.getMsg("confirm-publish.confirmation.unpublish")}
<#else>
${vrtx.getMsg("confirm-publish.confirmation.publish")}
</#if>
&nbsp;<span class="vrtx-confirm-publish-name"> ${name}</span>? 
</div>

<form name="vrtx-publish-resource" id="vrtx-publish-resource" action="${url}" method="post">
  <div class="submitButtons">
    <div class="vrtx-focus-button">
      <input tabindex="1" type="submit" id="publishResourceAction" 
             name="publishResourceAction" value="${vrtx.getMsg("confirm-delete.ok")}" />
    </div>
    <div class="vrtx-button">
      <input tabindex="2" type="submit" onclick="tb_remove(); return false;" id="publishResourceCancelAction"  
             name="publishResourceCancelAction" value="${vrtx.getMsg("confirm-delete.cancel")}"  />
    </div>
  </div>
</form>
</body>
</html>
