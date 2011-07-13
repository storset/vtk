<#ftl strip_whitespace=true>
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
  <!--[if lte IE 8]>
    <link rel="stylesheet" type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/report/jquery.treeview.ie.css" />
  <![endif]-->
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
    <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript">
     <!--
     $(document).ready(function(){
       var timestamp = 1 - new Date();
       $("#tree").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=subresource-retrieve&uri=${report.uri}&ts=" + timestamp
       })
       
       $("#tree").delegate("a", "click", function(e){
	       // don't want click on links
	       return false;
       });
       
       $("#tree").vortexTips("li", 180, 30); // class, leftOffset, topOffset
       
     });

     // -->
  </script>
  </head>
  <body id="vrtx-report">
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
	
	  <ul id="tree" class="filetree treeview-gray"></ul>
	  
	  <div id="vrtx-report-help">
	    <h2><@vrtx.msg code="report.collection-structure.help" /></h2>
	    <p id="vrtx-report-help-restricted">
	      <span id="vrtx-report-help-restricted-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.restricted" />
	    </p>
	    <p id="vrtx-report-help-allowed-for-all">
	      <span id="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.allowed-for-all" />
	    </p>
	    <p id="vrtx-report-help-inherited">
	      <span id="vrtx-report-help-allowed-for-all-inherited-folder"></span>
	      <span id="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.inherited" />
	    </p>
	    <p id="vrtx-report-help-not-inherited">
	      <span id="vrtx-report-help-allowed-for-all-not-inherited-folder"></span>
	      <span id="vrtx-report-help-allowed-for-all-not-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.not-inherited" />
	    </p>
	  </div>
	</div>
  </div>
  </body>
</html>

<#-- SUBRESOURCES TRUCTURE BUILD (keep) -->

<#macro displaySubResourceStructure subResourceStructure>
      <div class="vrtx-subfolder-menu">
        <ul id="tree" class="filetree treeview-gray">
          <#assign i = 0 />
          <#assign size = subResourceStructure?size />
          <#list subResourceStructure as item>
          
            <#assign listClasses = "" />
            <#if (i == (size-1))>
              <#assign listClasses = listClasses + "last" />
            </#if>
            <#if !item.inheritedAcl>
              <#assign listClasses = listClasses +  " not-inherited" />
            </#if>
            <#if item.collection>
              <#assign listClasses = listClasses +  " closed hasChildren" />
            </#if>
            <li class="${listClasses}">
             -->
              <#assign spanClasses = "" />
              <#if item.collection>
                <#assign spanClasses = spanClasses +  "folder" />
              <#else>
                <#assign spanClasses = spanClasses +  "file" />
              </#if>
              <#if item.readRestricted>
                <#assign spanClasses = spanClasses + " restricted">
              <#else>
                <#assign spanClasses = spanClasses + " allowed-for-all">
              </#if>
              <span class="${spanClasses}">
                <a href="${item.uri?html}" title="${item.title?html}">${item.name?html}</a>
              </span>
              <#if item.collection>
                <ul><li></li></ul>
              </#if>
            </li>
            <#assign i = i + 1 />
          </#list>
        </ul>
      </div>
</#macro>
