<#ftl strip_whitespace=true>

<#--
  - File: about-resource.ftl
  - 
  - Description: A HTML page that displays information about a resource
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>About resource</title>
</head>
<body>

  <div style="padding-left:0.5em;padding-right:0.5em;padding-bottom:1em;">
    <#include "components/resource-detail.ftl"/>
    <#include "components/resource-properties.ftl"/>
  </div>

</body>
</html>
