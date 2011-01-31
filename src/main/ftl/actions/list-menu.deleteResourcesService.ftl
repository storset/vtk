<#attempt>
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<script type="text/javascript">
<!--  

  var deleteUncheckedMessage = '${vrtx.getMsg("tabMenu2.deleteUnCheckedMessage")}';         
  var confirmDelete = '${vrtx.getMsg("tabMenu2.deleteResourcesMessage")}';         
  var confirmDeleteAnd = '${vrtx.getMsg("tabMenu2.deleteResourcesAnd")}';
  var confirmDeleteMore = '${vrtx.getMsg("tabMenu2.deleteResourcesMore")}';
  
 //-->
</script>

<#recover>
${.error}
</#recover>