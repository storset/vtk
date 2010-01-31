
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
  
  </head>
  <body>
  
  <div class="vrtx-report">
  <#list reporters as reporter>
    <li class="vrtx-reporters">
      <ul><a href="${reporter.url}"><@vrtx.msg code="report.${reporter.name}" default="${reporter.name}" /></a></ul>
    </li>
  </#list>
  </div>
  
  </body>
</html>
