<#ftl strip_whitespace=true>

<#--
  - File: properties-listing.ftl
  - 
  - Description: A HTML page that displays resource properties
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Stats</title>
  </head>
  <body>
    <#list managementStats?keys as key>
      <h3>${key?html}</h3>
<pre>
<#list managementStats[key]?keys as itemKey>${itemKey?html}: ${managementStats[key][itemKey]?html}</#list>
</pre>
    </#list>
  </body>
</html>
