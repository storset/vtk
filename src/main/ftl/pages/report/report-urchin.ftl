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
  <body id="vrtx-report-urchin">
    <div class="resourceInfo">
      <div class="vrtx-report-nav">
        <div class="back">
          <a href="${serviceURL?html}" ><@vrtx.msg code="report.back" default="Back" /></a>
        </div>
      </div>
      <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
      <div class="report-urchin-div" id="report-urchin-div-total">
        <h3><@vrtx.msg code="report.${report.reportname}.total" /></h3>
        <table class="report-urchin-table" id="report-urchin-table-total">
          <tr class="report-urchin-table-row-top">
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-count-top">
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-number-top">
              <@vrtx.msg code="report.${report.reportname}.number" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesTotal as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="report-urchin-tablerow report-urchin-tablerow-${rowType}">
            <td class="report-urchin-table-data" id="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-title">
              <a href="${report.urlsTotal[title_index]?html}">${title?html}</a>
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-number">
              ${report.numbersTotal[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
      <div class="report-urchin-div" id="report-urchin-div-sixty">
        <h3><@vrtx.msg code="report.${report.reportname}.sixty" /></h3>
        <table class="report-urchin-table" id="report-urchin-table-sixty">
          <tr class="report-urchin-table-row-top">
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-count-top">
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-number-top">
              <@vrtx.msg code="report.${report.reportname}.number" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesSixty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="report-urchin-tablerow report-urchin-tablerow-${rowType}">
            <td class="report-urchin-table-data" id="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-title">
              <a href="${report.urlsSixty[title_index]?html}">${title?html}</a>
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-number">
              ${report.numbersSixty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
      <div class="urchin-report-div" id="report-urchin-div-thirty">
        <h3><@vrtx.msg code="report.${report.reportname}.thirty" /></h3>
        <table class="report-urchin-table" id="report-urchin-table-thirty">
          <tr class="report-urchin-table-row-top">
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-count-top">
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="report-urchin-table-data-top" id="report-urchin-table-data-number-top">
              <@vrtx.msg code="report.${report.reportname}.number" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesThirty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="report-urchin-tablerow report-urchin-tablerow-${rowType}">
            <td class="report-urchin-table-data" id="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-title">
              <a href="${report.urlsThirty[title_index]?html}">${title?html}</a>
            </td>
            <td class="report-urchin-table-data" id="report-urchin-table-data-number">
              ${report.numbersThirty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
    </div>
  </body>
</html>