<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.copyUnCheckedMessage",
         "You must check at least one element to copy") />
<script type="text/javascript"><!--  
  var copyUncheckedMessage = '${unCheckedMessage}';
 //-->
</script>

<#recover>
${.error}
</#recover>

