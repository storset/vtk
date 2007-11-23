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

    <style type="text/css">
      img.introduction-image {
        float: left;
        margin: 0 0.75em 0.5em 0;
        border:1px solid #ddd; 
      }
    </style>
</head>
<body>

  <h1>${title}</h1>

  <#if error?exists>
    <p>${error}</p>
  </#if>
  <#if resources?exists>
  <ul>
    <#list resources as resource>
      <div>
      <p><a href="${urls[resource_index]?html}">${resource.getPropertyByPrefix("","title").getFormattedValue()?html}</a></p>
        <#assign introProp = resource.getPropertyByPrefix("","introduction")?default("") />
        <#assign introImageProp = resource.getPropertyByPrefix("","picture")?default("") />
        <#if introImageProp != "">
          <img class="introduction-image" width="100" height="100" src="${introImageProp.formattedValue}" />
        </#if>        
        <#if introProp != "">
          <div>${introProp.formattedValue}</div>
        </#if>
      </div>
    </#list>
  </ul>
  </#if>
</body>
