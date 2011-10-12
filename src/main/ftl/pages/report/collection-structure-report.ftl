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
  <script type="text/javascript"><!--
     $(document).ready(function(){
       var timestamp = 1 - new Date();
       
       $("#tree li").remove();
       
       $("#tree").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=subresource-retrieve&uri=${report.uri}&ts=" + timestamp,
         service: "subresource-retrieve"
       })
       
       $("#tree").delegate("a", "click", function(e){ // Don't want click on links
	     return false;
       });
       
       // Params: class, appendTo, containerWidth, in-, pre-, outdelay, xOffset, yOffset, autoWidth, extra
       $("#tree").vortexTips("li a", "#contents", 400, 300, 4000, 3000, 30, 80, false, false);
     });
  // -->
  </script>
  </head>
  <body id="vrtx-report">
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
  	  <div class="back">
	    <a href="${serviceURL?html}"><@vrtx.msg code="report.back" default="Back" /></a>
	  </div>
	</div> 
	<h2><@vrtx.msg code="report.collection-structure" /></h2>
	<p>
	  <@vrtx.msg code="report.collection-structure.about" />
	</p>
	<div class="vrtx-report vrtx-permission-tree">
	
	  <ul id="tree" class="filetree treeview-gray"><li>null</li></ul>
	  
	  <div id="vrtx-report-help">
	    <h2><@vrtx.msg code="report.collection-structure.help" /></h2>
	    <p id="vrtx-report-help-restricted">
	      <span class="vrtx-report-help-restricted-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.restricted" />
	    </p>
	    <p id="vrtx-report-help-allowed-for-all">
	      <span class="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.allowed-for-all" />
	    </p>
	    <#--
	    <p id="vrtx-report-help-inherited">
	      <span class="vrtx-report-help-allowed-for-all-inherited-folder"></span>
	      <span class="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.inherited" />
	    </p>-->
	    <p id="vrtx-report-help-not-inherited">
	      <span class="vrtx-report-help-allowed-for-all-not-inherited-folder"></span>
	      <span class="vrtx-report-help-allowed-for-all-not-inherited-file"></span>
	      <@vrtx.msg code="report.collection-structure.help.not-inherited" />
	    </p>
	  </div>
	</div>
  </div>
  </body>
</html>