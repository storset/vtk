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
      <div class="report-urchin" id="report-urchin-total">
        <h3><@vrtx.msg code="report.${report.reportname}.total" /></h3>
        <#if report.titlesTotal?exists>
        <#if report.titlesTotal?has_content>
        <table class="report-urchin-table" id="report-urchin-table-total">
          <thead>
            <tr>
              <th class="report-urchin-table-data-count">Nr.</th>
              <th class="report-urchin-table-data-title">
                <@vrtx.msg code="report.${report.reportname}.title" />
              </th>
              <th class="report-urchin-table-data-number">
                <@vrtx.msg code="report.${report.reportname}.number" />
              </th>
            </tr>
          </thead>
          <tbody>
          <#assign count = 1>
          <#list report.titlesTotal as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <#if (title_index == 0)>
            <#assign rowType = rowType + " first" />
          </#if>
          <tr class="report-urchin-table-row ${rowType}">
            <td class="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data-title">
              <a href="${report.urlsTotal[title_index]?html}">${title?html}</a>
            </td>
            <td class="report-urchin-table-data-number">
              ${report.numbersTotal[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
          </tbody>
        </table>
        <#else>
        <div>
          <p><@vrtx.msg code="report.urchin.nodata" /></p>
        </div>
        </#if>
        <#else>
        <div>
          <p><@vrtx.msg code="report.urchin.error" /></p>
        </div>
        </#if>
      </div>
      <div class="report-urchin" id="report-urchin-sixty">
        <h3><@vrtx.msg code="report.${report.reportname}.sixty" /></h3>
        <#if report.titlesSixty?exists>
        <#if report.titlesSixty?has_content>
        <table class="report-urchin-table" id="report-urchin-table-sixty">
          <thead>
            <tr>
              <th class="report-urchin-table-data-count">Nr.</th>
              <th class="report-urchin-table-data-title">
                <@vrtx.msg code="report.${report.reportname}.title" />
              </th>
              <th class="report-urchin-table-data-number">
                <@vrtx.msg code="report.${report.reportname}.number" />
              </td>
            </tr>
          </thead>
          <tbody>
          <#assign count = 1>
          <#list report.titlesSixty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <#if (title_index == 0)>
            <#assign rowType = rowType + " first" />
          </#if>
          <tr class="report-urchin-table-row ${rowType}">
            <td class="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data-title">
              <a href="${report.urlsSixty[title_index]?html}">${title?html}</a>
            </td>
            <td class="report-urchin-table-data-number">
              ${report.numbersSixty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
          </tbody>
        </table>
        <#else>
        <div>
          <p><@vrtx.msg code="report.urchin.nodata" /></p>
        </div>
        </#if>
        <#else>
        <div>
          <p><@vrtx.msg code="report.urchin.error" /></p>
        </div>
        </#if>
      </div>
      <div class="report-urchin last" id="report-urchin-thirty">
        <h3><@vrtx.msg code="report.${report.reportname}.thirty" /></h3>
        <#if report.titlesThirty?exists>
        <#if report.titlesThirty?has_content>
        <table class="report-urchin-table" id="report-urchin-table-thirty">
          <thead>
            <tr>
              <th class="report-urchin-table-data-count">Nr.</th>
              <th class="report-urchin-table-data-title">
                <@vrtx.msg code="report.${report.reportname}.title" />
              </th>
              <th class="report-urchin-table-data-number">
                <@vrtx.msg code="report.${report.reportname}.number" />
              </th>
            </tr>
          </thead>
          <tbody>
          <#assign count = 1>
          <#list report.titlesThirty as title >
          <#assign rowType = "odd" />
          <#if (title_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>
          <#if (title_index == 0)>
            <#assign rowType = rowType + " first" />
          </#if>
          <tr class="report-urchin-table-row ${rowType}">
            <td class="report-urchin-table-data-count">
              ${count?html}.
            </td>
            <td class="report-urchin-table-data-title">
              <a href="${report.urlsThirty[title_index]?html}">${title?html}</a>
            </td>
            <td class=="report-urchin-table-data-number">
              ${report.numbersThirty[title_index]?html}
            </td>
          </tr>
          <#assign count = count + 1>
          </#list>
          </tbody>
        </table>
        <#else>
          <div>
            <p><@vrtx.msg code="report.urchin.nodata" /></p>
          </div>
        </#if>
        <#else>
          <div>
            <p><@vrtx.msg code="report.urchin.error" /></p>
          </div>
        </#if>
      </div>
    </div>
  </body>
</html>