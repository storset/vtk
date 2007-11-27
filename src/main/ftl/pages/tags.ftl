<#ftl strip_whitespace=true>

<#--
  - File: tags.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />


<#assign title>
  <@vrtx.msg code="tags.title" args=[tag] />
</#assign>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  
  <style type="text/css">
    ul.tag-element-list { 
      margin: 0px;
      padding: 0px 0px 10px 0px;
      display: block;
    }
    
    ul.tag-element-list li {
      list-style: none;
      clear: both;
      margin: 0px;
      padding: 10px 0px 5px 0px;
    }
    
    ul.tag-element-list li p {
      margin: 0px;
      padding: 0px;
    }
    
    ul.tag-element-list img {
      float: left;
      padding: 5px 10px 10px 0px;
      border: none;
    }
    
    ul.tag-element-list div.title {
      margin: 0px;
      padding: 5px 0px 0px 0px;
      font-size: 125%;
    }
    
    .italic {
      font-style: italic;
    }
  </style>
</head>

<body>
  <h1>${title}</h1>

  <#if error?exists>
    <p>${error}</p>
  </#if>
  
  <#if resources?exists && resources?has_content>
	  <ul class="tag-element-list">
	    <#list resources as resource>
	      <#assign resourceTitle = resource.getPropertyByPrefix("","title").getFormattedValue() />
	      <#assign introProp = resource.getPropertyByPrefix("","introduction")?default("") />
	      <#assign introImageProp = resource.getPropertyByPrefix("","picture")?default("") />
	        
	      <li>
            <#if introImageProp != "">
              <a href="${urls[resource_index]?html}">
                <img class="introduction-image" 
                     width="100" height="100"
                     alt="IMG for ${title}"
                     src="${introImageProp.formattedValue}" />
              </a>
            </#if>
          
            <div class="title">
              <a href="${urls[resource_index]?html}">
                ${resourceTitle?html}
              </a>
            </div>
          
            <#if introProp != "">
              ${introProp.formattedValue}
            </#if>
          </li>
	    </#list>
	  </ul>
	<#else>
      <p>
        ${vrtx.getMsg("tags.notFound")} <span class="italic">${tag}</span>.
      </p>
  </#if>
</body>
</html>