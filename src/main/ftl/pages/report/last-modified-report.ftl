
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
    <link rel="stylesheet" href="${cssURL}" />
    </#list>
  </#if>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  </head>
  <body>
  <div class="resourceInfo">
  <div class="vrtx-report-nav">
  	<div class="back"> 
    <a href="${serviceURL}" ><@vrtx.msg code="report.back" default="Back" /></a>
    </div>
  </div>
  <h2><@vrtx.msg code="report.last-modified" /></h2>
  <p>
  <@vrtx.msg code="report.last-modified.about" />
  </p>
  <div class="vrtx-report">

<#if report.lastModifiedList?exists >
    <table cellpadding="3" border="1">
      <thead>
        <tr>
          <th><@vrtx.msg code="report.title" default="Title" /></th>
          <th><@vrtx.msg code="report.last-modified" default="Last modified" /></th>
          <th><@vrtx.msg code="report.modified-by" default="Modified by" /></th>
          <th><@vrtx.msg code="report.permission-set" default="Permissions set" /></th> 
          <th><@vrtx.msg code="collectionListing.permissions" default="Permissions"/></th>
          <th><@vrtx.msg code="report.published" default="Published" /> </th>
        </tr>
      </thead>
      <tbody>   
      <#list report.lastModifiedList as lastModified>
      	<#assign title= vrtx.propValue(lastModified, 'title') />
      	<#assign lastModifiedTime = vrtx.propValue(lastModified, 'lastModified') />
      	<#assign modifiedBy = vrtx.propValue(lastModified, 'modifiedBy') /> 
        <#assign aclIsInherited = vrtx.getMsg("report.yes", "Yes")>
        <#if lastModified.isInheritedAcl() >
        	<#assign aclIsInherited = vrtx.getMsg("report.no", "No")>
        </#if>
        <#assign isReadRestricted = vrtx.getMsg("collectionListing.permissions.readAll") >
        <#if report.isReadRestricted[lastModified_index] >
        	<#assign isReadRestricted = vrtx.getMsg("collectionListing.permissions.restricted")>
        </#if>
        <#assign published = vrtx.propValue(lastModified, 'published') />
		<#assign publishedStatus = vrtx.getMsg("report.yes", "Yes")>
		<#if published = "false">
			<#assign publishedStatus = vrtx.getMsg("report.no", "No")>
		</#if>
        <tr>  
        <td><a href="${lastModified.URI}">${title}</a></td>
        <td>${lastModifiedTime}</td>
		<#if modifiedBy?index_of("@") != -1>
		<td>${modifiedBy}</td>
		  <#else>
		<td><a href="http://www.uio.no/sok?person=${modifiedBy}">${modifiedBy}</a></td>
		</#if>
        <td>${aclIsInherited}</td> 
        <td>${isReadRestricted}</td>  
        <td>${publishedStatus}</td>
      </tr>
      </#list>
      </tbody>
    </table>
  </div>
  
  </#if>
  
  
  </div>
  
  

  </body>
</html>