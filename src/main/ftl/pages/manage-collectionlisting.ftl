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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#-- Add table-tr hover support to IE6 browsers -->
  <title>Manage: collection listing</title>   
</head>
<body id="vrtx-manage-collectionlisting">
  <#assign copyTitle = vrtx.getMsg("tabMenuRight.copyResourcesService") />
  <#assign moveTitle = vrtx.getMsg("tabMenuRight.moveResourcesService") />
  <#assign deleteTitle = vrtx.getMsg("tabMenuRight.deleteResourcesService") />
  
  <#assign moveUnCheckedMessage = vrtx.getMsg("tabMenuRight.moveUnCheckedMessage",
         "You must check at least one element to move") />
         
  <#assign copyUnCheckedMessage = vrtx.getMsg("tabMenuRight.copyUnCheckedMessage",
         "You must check at least one element to copy") />
         
  <script type="text/javascript"><!-- 
    var moveUncheckedMessage = '${moveUnCheckedMessage}';
    var copyUncheckedMessage = '${copyUnCheckedMessage}';
    var deleteUncheckedMessage = '${vrtx.getMsg("tabMenuRight.deleteUnCheckedMessage")}';         
    var confirmDelete = '${vrtx.getMsg("tabMenuRight.deleteResourcesMessage")}';         
    var confirmDeleteAnd = '${vrtx.getMsg("tabMenuRight.deleteResourcesAnd")}';
    var confirmDeleteMore = '${vrtx.getMsg("tabMenuRight.deleteResourcesMore")}';
    var multipleFilesInfoText = '<strong>${vrtx.getMsg("tabMenuRight.fileUploadMultipleInfo.line1")}</strong><br />'
                            + '${vrtx.getMsg("tabMenuRight.fileUploadMultipleInfo.line2")}';
    var fileUploadMoreFilesTailMessage = '${vrtx.getMsg("tabMenuRight.fileUploadMoreFilesTailMessage")}';
  //-->
  </script>

  <@col.listCollection
     withForm=true
     action=action.submitURL?string
     submitActions={"copy-resources":copyTitle, "move-resources":moveTitle,"delete-resources":deleteTitle}/>
</body>
</html>