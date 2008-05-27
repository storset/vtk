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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <#if error?exists>
    <#assign title = vrtx.getMsg("tags.noTagTitle") />
  <#elseif scope?exists>
    <#assign title><@vrtx.msg code="tags.scopedTitle" args=[scope.title,tag] /></#assign>
  <#else>
    <#assign title><@vrtx.msg code="tags.title" args=[tag] /></#assign>
  </#if>

  <title>${title}</title>

  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
       <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
    </#list>
  </#if>
 
</head>

<body id="vrtx-tagview">
  <h1>${title}</h1>

  <#if error?exists>
    <p>${error}</p>

  <#else>

    <#if resources?exists && resources?has_content>
      <div class="tagged-resources">
        <#list resources as resource>
	  <#assign resourceTitle = resource.getPropertyByPrefix("","title").getFormattedValue() />
	  <#assign introProp = resource.getPropertyByPrefix("","introduction")?default("") />
	  <#assign introImageProp = resource.getPropertyByPrefix("","picture")?default("") />
	  
	  <div class="result">
            <#if introImageProp != "">
              <a href="${urls[resource_index]?html}">
                <img class="introduction-image" 
                     alt="IMG for ${title}"
                     src="${introImageProp.formattedValue}" />
              </a>
            </#if>
            
	    <h2 class="title">
              <a href="${urls[resource_index]?html}">
                ${resourceTitle?html}
              </a>
            </h2>
            
            <#if introProp != "">
	      <div class="description">
                ${introProp.formattedValue}
              </div>
            </#if>

          </div> <!-- end class result -->

        </#list>
      </div>
    <#else>
      <p>
        ${vrtx.getMsg("tags.notFound")} <span class="italic">${tag}</span>.
      </p>
    </#if>
  </#if>
</body>
</html>
