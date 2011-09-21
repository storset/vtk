<#ftl strip_whitespace=true>

<#--
  - File: manage-collectionlisting.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collectionlisting.ftl" as col />
<#import "/lib/ping.ftl" as ping />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#-- Add table-tr hover support to IE6 browsers -->
  <title>Manage: collection listing</title>
  <@ping.ping url=resourceContext.currentServiceURL interval=900 />     
</head>
<body id="vrtx-manage-collectionlisting">
  <#assign copyTitle = vrtx.getMsg("tabMenu2.copyResourcesService") />
  <#assign moveTitle = vrtx.getMsg("tabMenu2.moveResourcesService") />
  <#assign deleteTitle = vrtx.getMsg("tabMenu2.deleteResourcesService") />
  
  <#assign moveUnCheckedMessage = vrtx.getMsg("tabMenu2.moveUnCheckedMessage",
         "You must check at least one element to move") />
         
  <#assign copyUnCheckedMessage = vrtx.getMsg("tabMenu2.copyUnCheckedMessage",
         "You must check at least one element to copy") />
         
  <script type="text/javascript"><!-- 
    var moveUncheckedMessage = '${moveUnCheckedMessage}';
    var copyUncheckedMessage = '${copyUnCheckedMessage}';
    var deleteUncheckedMessage = '${vrtx.getMsg("tabMenu2.deleteUnCheckedMessage")}';         
    var confirmDelete = '${vrtx.getMsg("tabMenu2.deleteResourcesMessage")}';         
    var confirmDeleteAnd = '${vrtx.getMsg("tabMenu2.deleteResourcesAnd")}';
    var confirmDeleteMore = '${vrtx.getMsg("tabMenu2.deleteResourcesMore")}';
    var multipleFilesInfoText = '<strong>${vrtx.getMsg("tabMenu2.fileUploadMultipleInfo.line1")}</strong><br />'
                            + '${vrtx.getMsg("tabMenu2.fileUploadMultipleInfo.line2")}'; 
  //-->
  </script>

  <@col.listCollection
     withForm=true
     action=action.submitURL?string
     submitActions={"copy-resources":copyTitle, "move-resources":moveTitle,"delete-resources":deleteTitle}/>
</body>
</html>