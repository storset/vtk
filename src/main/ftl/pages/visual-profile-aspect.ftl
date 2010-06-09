<#ftl strip_whitespace=true>

<#--
  - File: visual-profile-aspect.ftl
  - 
  - Description: Editor page for the visual profile aspect in 'aspects' JSON property
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/propertyList.ftl" as propList />
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>Visual profile</title>
</head>
<body>
  <#if form.configError?exists>
    Error in configuration file: ${form.configError?html}
  <#else>
  <form action="${form.submitURL}" method="post">
    <#list form.elements as element>
      <#if element.type == 'flag'>
        <div>
          <input type="checkbox" name="${element.identifier?html}" value="true" <#if element.value?exists>checked="checked"</#if>> ${element.label?html}
        </div>
      <#elseif element.type == 'string'>
        <div>${element.label?html} <input type="text" name="${element.identifier?html}" value="${element.value?default('')?html}" /></div>
      <#elseif element.type == 'html'>
        HTML
      <#elseif element.type == 'enum'>
          ${element.label?html} : 
        <#if element.possibleValues?exists>
        <#list element.possibleValues as value>
          <input type="radio" name="${element.identifier?html}" value="${value.value?default('')?html}"<#if value.selected>checked="checked"</#if> />
          ${value.label?html} <#-- (${value.value?default('null')}, ${value.selected?string}) -->
        </#list>
        </#if>
      <#else>
        unknown type: ${element.type?html}
      </#if>
    </#list>
    <input type="submit" id="saveAction" name="saveAction" value="Save" default="Save" />
    <input type="submit" id="cancelAction" name="cancelAction" value="Cancel" default="Cancel" />
  </form>
  </#if>
</body>
</html>

