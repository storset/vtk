
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
  <body id="vrtx-report-documents">
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
      <div class="back">
        <a href="${report.backURL?html}" ><@vrtx.msg code="report.diagram" default="Back" /></a>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
  <#if (report.total?exists && report.total > 0)>
    <div class="vrtx-report-diagram">
      <h3><@vrtx.msg code="report.diagram.webtypetitle" /></h3>
      <div class="vrtx-report-diagram-table">
        <table id="vrtx-report-diagram-webtypes">
          <#list report.types as type>
          <tr>
            <td class="vrtx-report-diagram-text"><#if report.typeURL[type_index]?exists><a href="${report.typeURL[type_index]?html}"><@vrtx.msg code="report.webDiagram.${type}" /></a>
              <#else><@vrtx.msg code="report.webDiagram.${type}" /></#if></td>
            <td class="vrtx-report-diagram-count"><#if report.typeCount[type_index]?exists>${report.typeCount[type_index]}<#else>0</#if></td>
          </tr>
          </#list>
          <tr class="vrtx-report-diagram-total">
            <td class="vrtx-report-diagram-text"><@vrtx.msg code="report.diagram.total" /></td>
            <td class="vrtx-report-diagram-count">${report.total}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="webtypesimg" width="550" height="180" alt="<@vrtx.msg code="report.${report.reportname}.webpagepiechart" />" 
             src="https://chart.googleapis.com/chart?chs=550x180&cht=p3&chd=s:Sm&chdl=<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>|</#if><#if (report.typeCount[type_index] > 0)><@vrtx.msg code="report.webDiagram.+${type}" /></#if></#list>&chl=<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>|</#if><#if (report.typeCount[type_index] > 0)><@vrtx.msg code="report.webDiagram.+${type}" /></#if></#list>&chd=t:<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>,</#if><#if (report.typeCount[type_index] > 0)>${((report.typeCount[type_index]/report.total)*100)?string("0")}</#if></#list>" />
      </div>
    </div>
  </#if>
  <#if !((report.firsttotal?exists && report.firsttotal > 0) || (report.total?exists && report.total > 0))>
        <p><@vrtx.msg code="report.diagram.error" /></p>
  </#if>
  </div>
  </body>
</html>