<#ftl strip_whitespace=true>
<#--
  - File: list-resources.ftl
  - 
  - Description: List resources under a folder
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  <#if cssURLs?exists>
    <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}" type="text/css" />
    </#list>
  </#if>
  <!--[if lte IE 8]>
    <link rel="stylesheet" type="text/css" href="/vrtx/__vrtx/static-resources/themes/default/report/jquery.treeview.ie.css" type="text/css" />
  <![endif]-->
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript"><!--
     $(document).ready(function(){
       $(".vrtx-permission-tree").prepend("<ul id='tree' class='filetree treeview-gray'></ul>");
       $("#tree").treeview({
         animated: "fast",
         url: "?vrtx=admin&service=list-resources-retrieve&uri=${report.uri}&ts=" + (+new Date()),
         service: "list-resources-retrieve"
       })
       $("#tree").on("click", "a", function(e) { // Don't want click on links
	     e.preventDefault();
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
	<h2><@vrtx.msg code="report.list-resources" /></h2>
	<p>
	  <@vrtx.msg code="report.list-resources.about" />
	</p>
	<div class="vrtx-report vrtx-permission-tree">
	  <div id="vrtx-report-help">
	    <h2><@vrtx.msg code="report.list-resources.help" /></h2>
	    <p id="vrtx-report-help-restricted">
	      <span class="vrtx-report-help-restricted-inherited-file"></span>
	      <@vrtx.msg code="report.list-resources.help.restricted" />
	    </p>
	    <p id="vrtx-report-help-allowed-for-all">
	      <span class="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.list-resources.help.allowed-for-all" />
	    </p>
	    <#--
	    <p id="vrtx-report-help-inherited">
	      <span class="vrtx-report-help-allowed-for-all-inherited-folder"></span>
	      <span class="vrtx-report-help-allowed-for-all-inherited-file"></span>
	      <@vrtx.msg code="report.list-resources.help.inherited" />
	    </p>-->
	    <p id="vrtx-report-help-not-inherited">
	      <span class="vrtx-report-help-allowed-for-all-not-inherited-folder"></span>
	      <span class="vrtx-report-help-allowed-for-all-not-inherited-file"></span>
	      <@vrtx.msg code="report.list-resources.help.not-inherited" />
	    </p>
	  </div>
	</div>
  </div>
  </body>
</html>