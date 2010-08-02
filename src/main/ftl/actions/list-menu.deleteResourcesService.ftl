<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<script type="text/javascript" language="Javascript"><!--  
  var deleteUncheckedMessage = '${vrtx.getMsg("tabMenu2.deleteUnCheckedMessage",
         "You must check at least one element to delete")}';
         
  var confirmDelete = '${vrtx.getMsg("tabMenu2.deleteResourcesMessage",
         "Are you sure you want to delete (1) resource(s): ")}';
 //-->
</script>

<#recover>
${.error}
</#recover>