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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Available decorator components</title>
  <link rel="stylesheet" type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/list-decorator-components.css" />
</head>
<body>
  <h1>Available decorator components</h1>
  <#if componentList?exists>

  <#-- table of contents -->
  <div class="content">                         
  <h2>List of components</h2>
  <ul>
    <#list componentList?sort_by('name')?sort_by('namespace') as component>
      <#assign componentID = component.namespace + ":" + component.name />
      <#if !hiddenComponents?exists || !hiddenComponents?seq_contains(componentID)>
      <li><a href="#${componentID?html}">${componentID?html}</a></li>
      </#if>
      </#list>
  </ul>
  </div>
    <#list componentList?sort_by('name')?sort_by('namespace') as component>
      <#assign componentID = component.namespace + ":" + component.name />
      <#if !hiddenComponents?exists || !hiddenComponents?seq_contains(componentID)>
      <h2 id="${componentID?html}">
        ${componentID?html}
      </h2>
      <h3>Description</h3>
      <div class="componentDescription">${(component.description?html)?if_exists}</div>

      <#if (component.parameterDescriptions)?exists>
        <h3>Parameters</h3>
        <#list component.parameterDescriptions?keys as paramName>
          <dl class="parameters">
            <dt>${paramName}</dt> 
            <dd>${component.parameterDescriptions[paramName]?html}</dd>
          </dl>
        </#list>
      </#if>
      <#if (component.usageExamples)?exists>
        <h3>Examples</h3>
        <#list component.usageExamples as example>
          <dl class="examples">
            <dt>
              ${example.example(component.name)}
            </dt>
            <dd>
              <#if (example.description())?exists>
                ${example.description()?html}
              </#if>
            </dd>
          </dl>
        </#list>
      </#if>
      </#if>
   </#list>
  </#if>
</body>
</html>
