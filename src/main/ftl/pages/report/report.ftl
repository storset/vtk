<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/ping.ftl" as ping />

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
  <@ping.ping url=pingURL['url'] interval=900 />  
  <body id="vrtx-report">
    <div class="resourceInfo">
      <h2><@vrtx.msg code="report.heading" default="Reports" /></h2>
      <div class="vrtx-report">
      
        <#if primaryReporters?has_content>
          <ul class="vrtx-primary-reporters">
          <#list primaryReporters as reporter>
            <li class="${reporter.name}">
              <a href="${reporter.url}">
                <span class="vrtx-primary-reporters-title"><@vrtx.msg code="report.${reporter.name}" default="${reporter.name}" /></span>
                <img class="vrtx-primary-reporters-image" src="/vrtx/__vrtx/static-resources/themes/default/images/report-${reporter.name}.gif" alt="${reporter.name} icon" />
                <span class="vrtx-primary-reporters-info"><@vrtx.msg code="report.primaries.info.${reporter.name}" default="${reporter.name}" /></span>
              </a>
            </li>
          </#list>
  	      </ul>
  	    </#if>
  	    
  	    <#if reporters?has_content>
  	      <h4 class="vrtx-reporters-title"><@vrtx.msg code="report.heading.others" default="Other reports" /></h4>
          <ul class="vrtx-reporters">
          <#list reporters as reporter>
            <li class="${reporter.name}"><a href="${reporter.url}"><@vrtx.msg code="report.${reporter.name}" default="${reporter.name}" /></a></li>
          </#list>
  	      </ul>
  	    </#if>
  	    
      </div>
    </div>
  </body>
</html>