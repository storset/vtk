
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
        <a href="${serviceURL}" ><@vrtx.msg code="report.back" default="Back" /></a>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
  <#if (report.firsttotal?exists && report.firsttotal > 0)>
    <div class="vrtx-report-diagram">
      <h3><@vrtx.msg code="report.diagram.folderandfiletitle" /></h3>
      <div class="vrtx-report-diagram-table">
        <table id="vrtx-report-diagram-filesandfolders">
          <tr>
            <td class="vrtx-report-diagram-text"><a href="${report.foldersURL?html}"><@vrtx.msg code="report.diagram.folder" /></a></td>
            <td class="vrtx-report-diagram-count">${report.folders}</td>
          </tr>
          <tr>
            <td class="vrtx-report-diagram-text"><a href="${report.filesURL?html}"><@vrtx.msg code="report.diagram.file" /></a></td>
            <td class="vrtx-report-diagram-count">${report.files}</td>
          </tr>
          <tr class="vrtx-report-diagram-total">
            <td class="vrtx-report-diagram-text"><@vrtx.msg code="report.diagram.total" /></td>
            <td class="vrtx-report-diagram-count">${report.firsttotal}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="filesandfoldersimg" width="480" height="180" alt="<@vrtx.msg code="report.${report.reportname}.filesandfolderspiechart" />" 
             src="https://chart.googleapis.com/chart?chs=480x180&cht=p3&chd=s:Sm&chdl=<@vrtx.msg code="report.diagram.folder" />|<@vrtx.msg code="report.diagram.file" />&chl=<#if (report.folders > 0)><@vrtx.msg code="report.diagram.folder" /></#if>|<#if (report.files > 0)><@vrtx.msg code="report.diagram.file" /></#if>&chd=t:${((report.folders/report.firsttotal)*100)?string("0")},${((report.files/report.firsttotal)*100)?string("0")}" />
      </div>
    </div>
  </#if>
  <#if (report.secondtotal?exists && report.secondtotal > 0)>
    <div class="vrtx-report-diagram">
      <h3><@vrtx.msg code="report.diagram.filetypetitle" /></h3>
      <div class="vrtx-report-diagram-table">
        <table id="vrtx-report-diagram-filetypes">
          <#list report.types as type>
          <tr>
            <td class="vrtx-report-diagram-text"><#if report.typeURL[type_index]?exists><a href="${report.typeURL[type_index]?html}"><@vrtx.msg code="report.diagram.${type}" /></a>
              <#else><@vrtx.msg code="report.diagram.${type}" /></#if></td>
            <td class="vrtx-report-diagram-count"><#if report.typeCount[type_index]?exists>${report.typeCount[type_index]}<#else>0</#if></td>
          </tr>
          </#list>
          <tr class="vrtx-report-diagram-total">
            <td class="vrtx-report-diagram-text"><@vrtx.msg code="report.diagram.total" /></td>
            <td class="vrtx-report-diagram-count">${report.secondtotal}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="filetypesimg" width="480" height="180" alt="<@vrtx.msg code="report.${report.reportname}.filetypepiechart" />" 
             src="https://chart.googleapis.com/chart?chs=480x180&cht=p3&chd=s:Sm&chdl=<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>|</#if><#if (report.typeCount[type_index] > 0)><@vrtx.msg code="report.diagram.+${type}" /></#if></#list>&chl=<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>|</#if><#if (report.typeCount[type_index] > 0)><@vrtx.msg code="report.diagram.+${type}" /></#if></#list>&chd=t:<#list report.types as type><#if ((report.typeCount[type_index] > 0) && (type_index != 0))>,</#if><#if (report.typeCount[type_index] > 0)>${((report.typeCount[type_index]/report.secondtotal)*100)?string("0")}</#if></#list>" />
      </div>
    </div>
  </#if>
  <#if !((report.firsttotal?exists && report.firsttotal > 0) || (report.secondtotal?exists && report.secondtotal > 0))>
        <p><@vrtx.msg code="report.diagram.error" /></p>
  </#if>
  </div>
  </body>
</html>