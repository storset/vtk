<#ftl strip_whitespace=true>

<#--
  - File: tags.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -   tag
-->

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-tags.ftl" as tags />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  
  <#if tagElements?exists>
    <#if !scope.URI.root>
      <#assign title><@vrtx.msg code="tags.serviceTitle" args=[scope.title] /></#assign>
    <#else>
  	  <#assign title>${vrtx.getMsg("tags.noTagTitle")}</#assign>
  	</#if>
  <#elseif listing?exists && listing?has_content >
    <#assign scopeParam = scope.title />
    <#if scope.URI.root>
      <#assign scopeParam = repositoryID />
    </#if>
    <#assign msgKey = "tags.title" />
    <#if resourceType?exists>
      <#assign resourceTypeKey = msgKey + "." + resourceType />
      <#if resourceTypeKey != vrtx.getMsg(resourceTypeKey)>
        <#assign msgKey = resourceTypeKey />
      </#if>
    </#if>
    <#assign title><@vrtx.msg code=msgKey args=[scopeParam, tag] /></#assign>
  </#if>
  
  <title>${title?html}
    <#if page?has_content>
      <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
    </#if>
  </title>
  
  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
       <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
    </#list>
  </#if>
  
  <#if alternativeRepresentations?exists>
    <#list alternativeRepresentations as alt>
    <link rel="alternate" type="${alt.contentType?html}" title="${alt.title?html}" href="${alt.url?html}" />
    </#list>
  </#if>
 
  <meta name="robots" content="noindex"/> 
 
</head>

<body id="vrtx-tagview">
  <h1>${title}
    <#if page?has_content>
      <#if "${page}" != "1"> - <@vrtx.msg code="viewCollectionListing.page" /> ${page}</#if>
    </#if>
  </h1>  

  <#if tagElements?exists>
    <@tags.displayTagElements tagElements />
  <#else>
    <#if listing?exists && listing.hasContent() >
      <@tags.displayTagListing listing />
    <#else>
      <p>${vrtx.getMsg("tags.notFound")} <span class="italic">${tag?html}</span>.</p>
    </#if>
  </#if>

</body>
</html>