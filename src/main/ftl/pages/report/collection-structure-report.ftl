<#--
  - File: collection-structure-report.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subResourceStructure
  -
  -->

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
  <script type="text/javascript">
     <!--
     $(window).ready(function(){
       $("#tree").treeview({
         animated: "fast"
       });

       // Working AJAX-test
       //$("#tree2").treeview({
	     //url: "?vrtx=admin&service=subresource-retrieve&uri=/web/blogg"
	   //})
     });
     // -->
  </script>
  </head>
  <body>
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
  	  <div class="back">
	    <a href="${serviceURL}"><@vrtx.msg code="report.back" default="Back" /></a>
	  </div>
	</div> 
	<h2><@vrtx.msg code="report.collection-structure" /></h2>
	<p>
	  <@vrtx.msg code="report.collection-structure.about" />
	</p>
	<div class="vrtx-report">
	
	<#-- Working AJAX-test 
	<div class="vrtx-subfolder-menu">
	  <ul id="tree2" class="filetree treeview-gray">
	  </ul>
	</div> -->
	
	  <@displaySubResourceStructure report.subResourceStructure />
	  
	  <div id="vrtx-report-help">
	    <h2><@vrtx.msg code="report.collection-structure.help" /></h2>
	    <p id="vrtx-report-help-restricted"><@vrtx.msg code="report.collection-structure.help.restricted" /></p>
	    <p id="vrtx-report-help-allowed-for-all"><@vrtx.msg code="report.collection-structure.help.allowed-for-all" /></p>
	    <p id="vrtx-report-help-inherited"><@vrtx.msg code="report.collection-structure.help.inherited" /></p>
	    <p id="vrtx-report-help-not-inherited"><span id="vrtx-report-help-not-inherited-folder"></span><@vrtx.msg code="report.collection-structure.help.not-inherited" /></p>
	  </div>
	</div>
  </div>
  </body>
</html>iletree 

<#-- RECURSIVE MENU BUILD -->

<#macro displaySubResourceStructure subResourceStructure>
      <div class="vrtx-subfolder-menu">
        <ul id="tree" class="filetree treeview-gray">
          <#assign i = 0 />
          <#assign size = subResourceStructure?size />
          <#list subResourceStructure as item>
            <#if (i == (size-1))>
              <#if item.inheritedAcl>
                <li class="closed last">
              <#else>
                <li class="closed last not-inherited">
              </#if>
            <#else>
              <#if item.inheritedAcl>
                <li>
               <#else>
                <li class="not-inherited">
              </#if>
            </#if>
            <#if item.collection>
              <#if item.readRestricted>
                <span class="folder restricted">
              <#else>
                <span class="folder allowed-for-all">
              </#if>
            <#else>
              <#if item.readRestricted>
                <span class="file restricted">
              <#else>
                <span class="file allowed-for-all">
              </#if>
            </#if>
                <a class="retrieve" href="${item.uri?html}" title="${item.title?html}">${item.name?html}</a>
              </span>
            </li>
            <#assign i = i + 1 />
          </#list>
        </ul>
      </div>
</#macro>