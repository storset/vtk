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
    <#if jsURLs?exists>
      <#list jsURLs as jsURL>
        <script type="text/javascript" src="${jsURL}"></script>
      </#list>
    </#if>
  </head>
  <body>
    <#if thisMonth?exists && (ursTotal > 0)>
      <#assign months = [vrtx.getMsg("jan"), vrtx.getMsg("feb"), vrtx.getMsg("mar"), vrtx.getMsg("apr"), vrtx.getMsg("may"), vrtx.getMsg("jun"),
        vrtx.getMsg("jul"), vrtx.getMsg("aug"), vrtx.getMsg("sep"), vrtx.getMsg("oct"), vrtx.getMsg("nov"), vrtx.getMsg("dec")]>

      <#assign thisMonthBak = thisMonth>
      <#assign width = 360 + (20 * (ursNMonths + 1))>

       <ul>
         <li>
           <#list hosts as host><a href="${host?html}">${hostnames[host_index]?html}</a> </#list>
         </li>
       </ul>
       <div id="vrtx-resource-visit">
         <div id="vrtx-resource-visit-chart">
           <img id="vrtx-resource-visit-chart-image" width="${width}" height="225" alt="Visit chart"
             src="http://chart.apis.google.com/chart?chts=676767,15&chxs=0,676767,13,0,l,676767|1,676767,13,0,l,676767&chl=<#list 0..ursNMonths as i>${months[thisMonth]}<#if i != ursNMonths>|</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thisMonth = 11></#if></#list>&amp;chxr=0,28.333,${ursMonths[12]?string("0")}&amp;chxt=y,x&amp;chbh=a&amp;chs=${width}x225&amp;cht=bvg&amp;chco=ed1c24&amp;chds=0,${ursMonths[12]?string("0")}&amp;chd=t:<#assign thisMonth = thisMonthBak><#list 0..ursNMonths as i>${ursMonths[thisMonth]?string("0")}<#if i != ursNMonths>,</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thisMonth = 11></#if></#list>&amp;chtt=<@vrtx.msg code="resource.metadata.about.visit.last${ursNMonths}months" />" />
         </div>
         <div id="vrtx-resource-visit-stats">
           <div class="vrtx-resource-visit-stat first" id="vrtx-resource-visit-total">
             <span>${ursTotal}</span> <@vrtx.msg code="resource.metadata.about.visit.total" />
           </div>
           <div class="vrtx-resource-visit-stat" id="vrtx-resource-visit-thirty">
             <span>${ursThirtyTotal}</span> <@vrtx.msg code="resource.metadata.about.visit.thirty" />
           </div>
           <div class="vrtx-resource-visit-stat" id="vrtx-resource-visit-week">
             <span>${ursWeekTotal}</span> <@vrtx.msg code="resource.metadata.about.visit.week" />
           </div>
           <div class="vrtx-resource-visit-stat last" id="vrtx-resource-visit-yesterday">
             <span>${ursYesterdayTotal}</span> <@vrtx.msg code="resource.metadata.about.visit.yesterday" />
           </div>
         </div>
       </div>
    <#else>
      <p><@vrtx.msg code="resource.metadata.about.visit.nostats" /></p>
    </#if>
  </body>
  </html>
