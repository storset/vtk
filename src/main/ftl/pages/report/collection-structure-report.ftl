
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/layouts/subfolder-menu.ftl" as subfolder />

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
	  <@subfolder.displaySubFolderMenu report.subFolderMenu true true />
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