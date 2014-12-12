<#ftl strip_whitespace=true>
<#import "/lib/vtk.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#assign propName = vrtx.getMsg("property." + property, property)?lower_case />
  <#assign title = vrtx.getMsg("property.edit", "Edit " + propName, [propName]) />
  <title>${title?html}</title>
</head>
<body id="vrtx-property-edit">
  <h3>${title?html}</h3>
  <form action="${(form.action)?html}" method="post">
    <#list form.inputs as input>
      <input name="${(input.name)?default('')?html}" type="${(input.type)?default('')?html}"
             value="${(input.value)?default('')?html}" />
    </#list>
  </form>
</body>
