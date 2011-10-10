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
  </head>
  <body id="vrtx-report-diagram">
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
      <div class="back">
        <a href="${serviceURL?html}" ><@vrtx.msg code="report.back" default="Back" /></a>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
  <#if (report.thirdtotal?exists && report.thirdtotal > 0)>
    <div class="vrtx-report-diagram">
      <h3><@vrtx.msg code="report.diagram.webtypetitle" /></h3>
      <div class="vrtx-report-diagram-table">
        <table id="vrtx-report-diagram-webtypes">
          <#list report.webTypes as type>
          <tr>
            <td class="vrtx-report-diagram-text"><#if report.webTypeURL[type_index]?exists><a href="${report.webTypeURL[type_index]?html}"><@vrtx.msg code="report.webDiagram.${type}" /></a>
              <#else><@vrtx.msg code="report.webDiagram.${type}" /></#if></td>
            <td class="vrtx-report-diagram-count"><#if report.webTypeCount[type_index]?exists>${report.webTypeCount[type_index]}<#else>0</#if></td>
          </tr>
          </#list>
          <tr class="vrtx-report-diagram-total">
            <td class="vrtx-report-diagram-text"><@vrtx.msg code="report.diagram.total" /></td>
            <td class="vrtx-report-diagram-count">${report.thirdtotal}</td>
          </tr>
        </table>
      </div>
      <div class="vrtx-report-diagram-img">
        <img id="webtypesimg" width="660" height="220" alt="<@vrtx.msg code="report.${report.reportname}.webpagepiechart" />" 
             src="https://chart.googleapis.com/chart?chco=ed1c24&amp;chs=660x220&amp;cht=p3&amp;chd=s:Sm&amp;chdl=<#assign first = 0><#list report.webTypes as type><#if (report.webTypeCount[type_index] > 0)><#if (first != 0)>|</#if><@vrtx.msg code="report.webDiagram.+${type}" /><#assign first = 1></#if></#list>&amp;chl=<#assign first = 0><#list report.webTypes as type><#if (report.webTypeCount[type_index] > 0)><#if (first != 0)>|</#if><@vrtx.msg code="report.webDiagram.+${type}" /><#assign first = 1></#if></#list>&amp;chd=t:<#assign first = 0><#list report.webTypes as type><#if (report.webTypeCount[type_index] > 0)><#if (first != 0)>,</#if>${((report.webTypeCount[type_index]/report.thirdtotal)*100)?string("0")}<#assign first = 1></#if></#list>" />
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
        <img id="filetypesimg" width="660" height="220" alt="<@vrtx.msg code="report.${report.reportname}.filetypepiechart" />" 
             src="https://chart.googleapis.com/chart?chco=ed1c24&amp;chs=660x220&amp;cht=p3&amp;chd=s:Sm&amp;chdl=<#assign first = 0><#list report.types as type><#if (report.typeCount[type_index] > 0)><#if (first != 0)>|</#if><@vrtx.msg code="report.diagram.+${type}" /><#assign first = 1></#if></#list>&amp;chl=<#assign first = 0><#list report.types as type><#if (report.typeCount[type_index] > 0)><#if (first != 0)>|</#if><@vrtx.msg code="report.diagram.+${type}" /><#assign first = 1></#if></#list>&amp;chd=t:<#assign first = 0><#list report.types as type><#if (report.typeCount[type_index] > 0)><#if (first != 0)>,</#if>${((report.typeCount[type_index]/report.secondtotal)*100)?string("0")}<#assign first = 1></#if></#list>" />
      </div>
    </div>
  </#if>
  <#if (report.firsttotal?exists && report.firsttotal > 0)>
    <div class="vrtx-report-diagram vrtx-report-diagram-last">
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
        <img id="filesandfoldersimg" width="660" height="220" alt="<@vrtx.msg code="report.${report.reportname}.filesandfolderspiechart" />" 
             src="https://chart.googleapis.com/chart?chco=ed1c24&amp;chs=660x220&amp;cht=p3&amp;chd=s:Sm&amp;chdl=<@vrtx.msg code="report.diagram.folder" />|<@vrtx.msg code="report.diagram.file" />&amp;chl=<#if (report.folders > 0)><@vrtx.msg code="report.diagram.folder" /></#if>|<#if (report.files > 0)><@vrtx.msg code="report.diagram.file" /></#if>&amp;chd=t:${((report.folders/report.firsttotal)*100)?string("0")},${((report.files/report.firsttotal)*100)?string("0")}" />
      </div>
    </div>
  </#if>
  <#if !((report.firsttotal?exists && report.firsttotal > 0) || (report.secondtotal?exists && report.secondtotal > 0) || (report.thirdtotal?exists && report.thirdtotal > 0))>
        <p><@vrtx.msg code="report.diagram.error" /></p>
  </#if>
  </div>
  </body>
</html>