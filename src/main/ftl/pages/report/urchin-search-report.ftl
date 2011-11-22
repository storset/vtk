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
  <body id="vrtx-report-urchin-search">
    <div class="resourceInfo">
      <div class="vrtx-report-nav">
        <div class="back">
          <a href="${serviceURL?html}" ><@vrtx.msg code="report.back" default="Back" /></a>
        </div>
      </div>
      <h2><@vrtx.msg code="report.${report.reportname}" /></h2>
      <div class="urchin-search-report-div" id="urchin-search-report-div-total">
        <h3><@vrtx.msg code="report.${report.reportname}.total" /></h3>
        <table class="urchin-search-report-table" id="urchin-search-report-table-total">
          <tr class="urchin-search-report-table-row-top">
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-number-top">
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-visit-top">
              <@vrtx.msg code="report.${report.reportname}.visit" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesTotal as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="urchin-search-report-tablerow urchin-search-report-tablerow-${rowType}">
            <td class="urchin-search-report-table-data-number" id="urchin-search-report-table-data-number">
              ${count?html}.
            </td>
            <td class="urchin-search-report-table-data" id="urchin-search-report-table-data-title">
              <a href="${report.urlsTotal[title_index]?html}">${title?html}</a>
            </td>
            <td class="urchin-search-report-table-visit" id="urchin-search-report-table-data-visit">
              ${report.visitsTotal[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
      <div class="urchin-search-report-div" id="urchin-search-report-div-sixty">
        <h3><@vrtx.msg code="report.${report.reportname}.sixty" /></h3>
        <table class="urchin-search-report-table" id="urchin-search-report-table-total">
          <tr class="urchin-search-report-table-row-top">
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-number-top">
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-visit-top">
              <@vrtx.msg code="report.${report.reportname}.visit" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesSixty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="urchin-search-report-tablerow urchin-search-report-tablerow-${rowType}">
            <td class="urchin-search-report-table-data-number" id="urchin-search-report-table-data-number">
              ${count?html}.
            </td>
            <td class="urchin-search-report-table-data" id="urchin-search-report-table-data-title">
              <a href="${report.urlsSixty[title_index]?html}">${title?html}</a>
            </td>
            <td class="urchin-search-report-table-visit" id="urchin-search-report-table-data-visit">
              ${report.visitsSixty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
      <div class="urchin-search-report-div" id="urchin-search-report-div-thirty">
        <h3><@vrtx.msg code="report.${report.reportname}.thirty" /></h3>
        <table class="urchin-search-report-table" id="urchin-search-report-table-total">
          <tr class="urchin-search-report-table-row-top">
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-number-top">
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-title-top">
              <@vrtx.msg code="report.${report.reportname}.title" />
            </td>
            <td class="urchin-search-report-table-data-top" id="urchin-search-report-table-data-visit-top">
              <@vrtx.msg code="report.${report.reportname}.visit" />
            </td>
          </tr>
          <#assign count = 1>
          <#list report.titlesThirty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <tr class="urchin-search-report-tablerow urchin-search-report-tablerow-${rowType}">
            <td class="urchin-search-report-table-data-number" id="urchin-search-report-table-data-number">
              ${count?html}.
            </td>
            <td class="urchin-search-report-table-data" id="urchin-search-report-table-data-title">
              <a href="${report.urlsThirty[title_index]?html}">${title?html}</a>
            </td>
            <td class="urchin-search-report-table-visit" id="urchin-search-report-table-data-visit">
              ${report.visitsThirty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
        </table>
      </div>
    </div>
  </body>
</html>