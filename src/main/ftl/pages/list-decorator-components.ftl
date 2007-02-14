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
    h1 {
       font-size: 95%;
    }
    tr.header {
       text-align: left;
       font-size: 95%;
    }
    td {
       border: 1px solid black;
    }
    .componentName, .parameterName {
       font-family: monospace;
    }
  </style>
</head>
<body>
  <h1>Decorator components</h1>
  <#if componentList?exists>
    <dl>
      <#list componentList?sort_by('namespace') as component>
        <dt class="componentName">${component.namespace?html}:${component.name?html}</dt>
        <dd>
          ${(component.description?html)?if_exists}
            <#if (component.parameterDescriptions)?exists>
              <dl>
                Parameters:
                <#list component.parameterDescriptions?keys as paramName>
                  <dt>${paramName}</dt>
                  <dd>${component.parameterDescriptions[paramName]}</dd>
                </#list>
              </dl>
            </#if>
        </dd>
      </#list>
    </dl>

    <table>
      <tr class="header"><th>Name</th><th>Description</th><th>Parameters</th></tr>
      <#list componentList?sort_by('namespace') as component>
      <tr>
        <td class="componentName">${component.namespace?html}:${component.name?html}</td>
        <td class="componentDescription">${(component.description?html)?if_exists}</td>
        <td class="parameterDescriptions">
            <#if (component.parameterDescriptions)?exists>
              <table>
                <#list component.parameterDescriptions?keys as paramName>
                  <tr>
                    <td class="parameterName">${paramName}</td><td>${component.parameterDescriptions[paramName]}</td>
                  </tr>
                </#list>
              </table>
            </#if>
        </td>
      </tr>
      </#list>
    </table>
  </#if>
</body>
</html>
