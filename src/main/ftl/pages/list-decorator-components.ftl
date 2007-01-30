<#ftl strip_whitespace=true>

<#--
  - File: list-decorator-components.ftl
  - 
  - Description: 
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Available decorator components</title>
  <style type="text/css">
    body {
       font-family: Arial, Helvetica, sans-serif;
    }
    tr.header {
       text-align: left;
    }
    td {
       border: 1px solid black;
    }
    .componentName {
       font-family: monospace;
    }
  </style>
</head>
<body>
  <#if componentList?exists>
    <table>
      <tr class="header"><th>Name</th><th>Description</th></tr>
      <#list componentList?sort_by('namespace') as component>
      <tr>
        <td class="componentName">${component.namespace?html}:${component.name?html}</td>
        <td class="componentDescription">${component.description?html?if_exists}</td>
      </tr>
      </#list>
    </table>
  </#if>
</body>
</html>
