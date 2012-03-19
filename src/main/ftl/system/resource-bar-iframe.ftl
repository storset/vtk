<#ftl strip_whitespace=true />
<#import "/lib/menu/list-menu.ftl" as listMenu />
<#import "/system/resource-bar.ftl" as resBar />

<#assign resource = resourceContext.currentResource />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Resource bar</title>
  <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default.css" type="text/css" /> 
  <!--[if IE 7]>
    <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css" /> 
  <![endif]--> 
  <!--[if lte IE 6]>
    <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css" /> 
  <![endif]--> 
  
  <style type="text/css">
    body {
      min-width: 0;
    }
  </style>
  
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/jquery-1.6.2.min.js"></script> 
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery-ui-1.8.8.custom.min.js"></script> 
  <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/admin-enhancements.js"></script> 
</head>
<body>

<@resBar.gen resource resourceMenuLeft resourceMenuRight />

</body>
</html>