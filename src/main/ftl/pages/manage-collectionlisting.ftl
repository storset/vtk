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
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/ie6-table-tr-hover.js"></script>
  <title>Manage: collection listing</title>
</head>
<body>
  <#assign copyTitle = vrtx.getMsg("tabMenu2.copyResourcesService") />
  <#assign moveTitle = vrtx.getMsg("tabMenu2.moveResourcesService") />

  <@col.listCollection
     withForm=true
     action=action.submitURL?string
     submitActions={"copy-resources":copyTitle, "move-resources":moveTitle}/>
</body>
</html>
