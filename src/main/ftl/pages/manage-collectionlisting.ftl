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
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#-- Add table-tr hover support to IE6 browsers -->
  <title>Manage: collection listing</title>
</head>
<body id="vrtx-manage-collectionlisting">
  <#assign copyTitle = vrtx.getMsg("tabMenu2.copyResourcesService") />
  <#assign moveTitle = vrtx.getMsg("tabMenu2.moveResourcesService") />
  <#assign deleteTitle = vrtx.getMsg("tabMenu2.deleteResourcesService") />

  <@col.listCollection
     withForm=true
     action=action.submitURL?string
     submitActions={"copy-resources":copyTitle, "move-resources":moveTitle,"delete-resources":deleteTitle}/>
</body>
</html>