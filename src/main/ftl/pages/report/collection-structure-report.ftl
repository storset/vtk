<#--
  - File: collection-structure-report.ftl
  - 
  - Description: Sub folder menu implementation
  - 
  - Required model data:
  -   subFolderMenu
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<#-- RECURSIVE MENU BUILD -->

<#macro displaySubFolderMenu subFolderMenu>
  
    <#if subFolderMenu.size &gt; 0>
      <#assign "counter" = 0>
      <#assign "counter2" = 0>
      <#assign "c" = 0>
         
      <#if subFolderMenu.resultSets?exists>
          <div class="vrtx-subfolder-menu">
          <#list subFolderMenu.resultSets as resultSet>
            <#assign counter = counter+1>
            <#assign counter2 = counter2+1>
            <#if subFolderMenu.groupResultSetsBy?exists && (subFolderMenu.groupResultSetsBy?number = counter2 || counter = 1)>
               <#assign "counter2" = 0>
               <#assign c = c+1>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=true subFolderMenu=subFolderMenu />
            <#else>
               <@displayParentMenu menu=resultSet currentCount=counter groupCount=c newDiv=false subFolderMenu=subFolderMenu />
            </#if>
          </#list>
        </div>
      </#if>
</#if>

</#macro>

<#-- MACROS: -->
<#macro displayItem item separator="none" >
  <#if item.menu?exists>
    <a href="${item.url?html}">${item.label?html}</a>
    </span>
    <@displaySubMenu item.menu displaySubMenu />
  <#else> 
      <a href="${item.url?html}">${item.label?html}</a>
  </#if>
</#macro>

<#macro displayParentMenu menu currentCount groupCount newDiv subFolderMenu >
  <#if newDiv>
    <#if currentCount != 1>
     </div>
    </#if>  
    <div class="vrtx-group-${groupCount?html}">
  </#if>
      <ul class="resultset-${currentCount?html} filetree treeview-gray">
    <#list menu.itemsSorted as item>
            <#if true>
              <li>
            <#else>
              <li class="not-inherited">
            </#if>
            <#if true>
              <span class="folder restricted">
            <#else>
              <span class="folder allowed-for-all">
            </#if>
        <@displayItem item=item />
      </li>
    </#list>
  </ul>
  
  <#if subFolderMenu.groupResultSetsBy?exists && subFolderMenu.groupResultSetsBy?number &gt; 0 && currentCount == subFolderMenu.resultSets?size>
     </div>
  </#if>
</#macro>

<#macro displaySubMenu menu displaySubMenu >
  <ul>
    <#assign i = 0 />
    <#assign sized = menu.itemsSorted?size />
    <#list menu.itemsSorted as item>
      <#if (i < sized)>
            <#if (i == (sized-1))>
              <#if true>
                <li class="closed last">
              <#else>
                <li class="closed last not-inherited">
              </#if>
            <#else>
              <#if true>
                <li class="closed">
              <#else>
                <li class="closed not-inherited">
              </#if>
            </#if>
              <#if true>
                <span class="folder restricted">
              <#else>
                <span class="folder allowed-for-all">
              </#if>
             <@displayItem item=item separator="none" />
            </li>
         <#else>
           <#break>
         </#if>
         <#assign i = i + 1 />
    </#list>
  </ul>
</#macro>


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
       $(".resultset-1").treeview({
         animated: "fast"
       });
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
	  <@displaySubFolderMenu report.subFolderMenu />
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
</html>