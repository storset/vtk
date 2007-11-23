<#ftl strip_whitespace=true>

<#--
  - File: article.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
  -->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<#assign title><@vrtx.msg code="tags.title" args=[tag] /></#assign>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
 <head><title>${title}</title>
</head>
<body>

  <h1>${title}</h1>
  <ul>
    <#list resources as resource>
      <li><a href="${urls[resource_index]?html}">${resource.getPropertyByPrefix("","title").getFormattedValue()?html}</a></li>
    </#list>
  </ul>

</body>
