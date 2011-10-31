<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />


  <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
  <html xmlns="http://www.w3.org/1999/xhtml">
  <head>

    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
      <link rel="stylesheet" href="${cssURL}" />
      </#list>
    </#if>
  
    <!--[if IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css"/> 
    <![endif]--> 
    <!--[if lte IE 6]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css"/> 
    <![endif]--> 
  
  </head>
  <body>

    <#if thisMonth?exists && (ursTotal > 0)>
      <#assign months = [vrtx.getMsg("jan"), vrtx.getMsg("feb"), vrtx.getMsg("mar"), vrtx.getMsg("apr"), vrtx.getMsg("may"), vrtx.getMsg("jun"),
        vrtx.getMsg("jul"), vrtx.getMsg("aug"), vrtx.getMsg("sep"), vrtx.getMsg("oct"), vrtx.getMsg("nov"), vrtx.getMsg("dec")]>

      <table id="vrtx-resourceVisit" class="resourceInfo">
        <tr>
          <td>
            <#list hosts as host><a href="${host?html}">${hostnames[host_index]?html}</a> </#list>
          </td>
        </tr>
        <tr>
          <td id="vrtx-resourceVisit-chart">
            <img id="vrtx-resourceVisit-chart-image" width="600" height="225" alt="Visit chart"
              src="http://chart.apis.google.com/chart?chl=<#list 0..11 as i>${months[thisMonth]}<#if i != 11>|</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thisMonth = 11></#if></#list>&chxr=0,28.333,${ursMonths[12]?string("0")}&chxt=y,x&chbh=a&chs=600x225&cht=bvg&chco=3D7930&chds=0,${ursMonths[12]?string("0")}&chd=t:<#list 0..11 as i>${ursMonths[thisMonth]?string("0")}<#if i != 11>,</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thismonth = 11></#if></#list>&chtt=<@vrtx.msg code="resource.metadata.about.visit.last12months" />" />
          <td>
        </tr>
        <tr>
          <td id="vrtx-resourceVisit-visits-total">
            ${ursTotal} <@vrtx.msg code="resource.metadata.about.visit.total" />
          </td>
          <td id="vrtx-resourceVisit-visits-thirty">
            ${ursThirtyTotal} <@vrtx.msg code="resource.metadata.about.visit.thirty" />
          </td>
          <td id="vrtx-resourceVisit-visits-week">
            ${ursWeekTotal} <@vrtx.msg code="resource.metadata.about.visit.week" />
          </td>
          <td id="vrtx-resourceVisit-visits-yesterday vrtx-resourceVisit-visits-last">
            ${ursYesterdayTotal} <@vrtx.msg code="resource.metadata.about.visit.yesterday" />
          </td>
        </tr>
      </table>
    <#else>
      <@vrtx.msg code="resource.metadata.about.visit.nostats" />
    </#if>


  </body>
  </html>
