<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<#assign unCheckedMessage = vrtx.getMsg("tabMenu2.moveUnCheckedMessage",
         "You must check at least one element to move") />

<script type="text/javascript"><!--  
  var moveUncheckedMessage = '${unCheckedMessage}';
 //-->
</script>


<#recover>
${.error}
</#recover>
