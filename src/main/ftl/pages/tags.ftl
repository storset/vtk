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


<#assign title>
  <@vrtx.msg code="tags.title" args=[tag] />
</#assign>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>

  <style type="text/css">
    ul.tag-element-list {margin:0;padding:0 0 10px 0;display:block;}
    ul.tag-element-list li{clear:both;margin:0;padding:10px 0 5px 0;list-style:none;}
    ul.tag-element-list li p{margin:0;padding:0;}
    ul.tag-element-list li ul {margin:0;padding:0;}

    ul.tag-element-list img{float:left;padding:3px 10px 10px 0;border:0px solid #fff;}
    
    /*
    img.introduction-image {
      float: left;
      margin: 0 0.75em 0.5em 0;
      border:1px solid #ddd; 
    }
     
    .entry {
      float:left;
    }
    */
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
	            <a href="${urls[resource_index]?html}" style="float:left;">
	              <img class="introduction-image" width="100" height="100" alt="image" src="${introImageProp.formattedValue}" />
	            </a>
	          </#if>
	          
	          <h2>
	            <a href="${urls[resource_index]?html}">${resourceTitle?html}</a>
	          </h2>
	          
	          <#if introProp != "">
	            ${introProp.formattedValue}
	          </#if>
	          
	        </li>
	    </#list>
	  </ul>
	<#else>
      <p>
        No resources tagged with "${tag}".
      </p>
  </#if>
</body>
</html>