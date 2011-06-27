
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
  <body id="vrtx-report">
    <div class="resourceInfo">
      <h2><@vrtx.msg code="report.heading" default="Reports" /></h2>
      <div class="vrtx-report">
        <ul class="vrtx-reporters">
         <#list reporters as reporter>
           <li><a href="${reporter.url}"><@vrtx.msg code="report.${reporter.name}" default="${reporter.name}" /></a></li>
         </#list>
  	    </ul>
      </div>
    </div>
  </body>
</html>
