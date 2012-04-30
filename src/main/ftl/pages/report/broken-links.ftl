<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" type="text/css" />
      </#list>
    </#if>
    <#global baseFolder = "/" />
    <#if resourceContext.parentURI?exists>
      <#global baseFolder = resourceContext.currentURI?html />
    </#if>
    <script type="text/javascript" src="/vrtx/__vrtx/static-resources/js/plugins/mustache.js"></script>
    <script type="text/javascript"><!--
      var filtersAdvancedShow = "<@vrtx.msg code='report.broken-links.filters.advanced.show' />",
          filtersAdvancedHide = "<@vrtx.msg code='report.broken-links.filters.advanced.hide' />",
          filtersAdvancedTitle = "<@vrtx.msg code='report.broken-links.filters.advanced.title' />",
          filtersAdvancedExcludeTitle = "<@vrtx.msg code='report.broken-links.filters.advanced.exclude-title' />",
          filtersAdvancedIncludeTitle = "<@vrtx.msg code='report.broken-links.filters.advanced.include-title' />",
          filtersAdvancedUpdate = "<@vrtx.msg code='report.broken-links.filters.advanced.update' />",
          browseBase = '${fckeditorBase.url?html}',
          browseBaseFolder = '${baseFolder}',
          browseBasePath = '${fckBrowse.url.pathRepresentation}';

      $(document).ready(function() {
        // Multiple fields interaction
        initMultipleInputFields();
        
        $.when(MULTIPLE_INPUT_FIELD_TEMPLATES_DEFERRED).done(function() {
          if($(".report-filters-folders-exclude").length) {
            loadMultipleInputFields("report-filters-folders-exclude", '${vrtx.getMsg("editor.add")}',
                                    '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}',
                                    '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', false, true);
          }   
          if($(".report-filters-folders-include").length) {                  
            loadMultipleInputFields("report-filters-folders-include", '${vrtx.getMsg("editor.add")}',
                                    '${vrtx.getMsg("editor.remove")}', '${vrtx.getMsg("editor.move-up")}',
                                    '${vrtx.getMsg("editor.move-down")}', '${vrtx.getMsg("editor.browseImages")}', false, true);
          } 
        }); 
      }); 
   // -->
   </script>
  </head>
  <body id="vrtx-report-broken-links">
  <#assign linkTypeLocalization>
    <@vrtx.msg code="report.${report.reportname}.filters.link-type.${report.linkType}" />
  </#assign>
  
  <div class="resourceInfo">
    <div class="vrtx-report-nav">
      <div class="back">
        <a href="${serviceURL?html}" ><@vrtx.msg code="report.back" default="Back" /></a>
      </div>
    </div>
    <h2><@vrtx.msg code="report.${report.reportname}.title" /> ${linkTypeLocalization?lower_case}</h2>
  </div>
  
  <@generateFilters report.filters />

  <#if (report.result?exists && report.result?size > 0)>
    <#--
      <p id="vrtx-report-broken-links-info">
        <span class="vrtx-report-broken-links-info-number">${report.brokenLinkCount} <@vrtx.msg code="report.${report.reportname}.info.total-broken-links-count" /></span>
        <span class="vrtx-report-broken-links-info-number">${report.total}</span> <@vrtx.msg code="report.${report.reportname}.info.web-pages.num" />
        <span class="vrtx-report-broken-links-info-number">7%</span> av nettsidene med brutte lenker
      </p>
    -->
    <p id="vrtx-report-info-paging-top">
      <@vrtx.msg code="report.${report.reportname}.about"
                 args=[report.from, report.to, report.total, report.brokenLinkCount]
                 default="Listing results " + report.from + "–"
                 + report.to + " of total " + report.total + " of web pages with " + report.brokenLinkCount + " broken" /> ${linkTypeLocalization?lower_case}
      <#if report.prev?exists || report.next?exists>
        <@displayPaging />  
      </#if>
    </p>
    <div class="vrtx-report">
      <table id="directory-listing" class="report-broken-links">
        <thead>
          <tr>
            <th id="vrtx-report-broken-links-web-page"><@vrtx.msg code="report.${report.reportname}.web-page" /></th>
            <th id="vrtx-report-broken-links-count"><@vrtx.msg code="report.${report.reportname}.count" /></th>
            <th id="vrtx-report-broken-links">${linkTypeLocalization} <@vrtx.msg code="report.${report.reportname}.list" /></th>
            <th id="vrtx-report-last-checked"><@vrtx.msg code="report.broken-links.last-checked" default="Last checked" /></th>
          </tr>
        </thead>
        <tbody>
        <#assign brokenLinksSize = report.result?size />
        <#list report.result as item>
          <#assign title = vrtx.propValue(item, 'title') />
          <#assign url = "" />
          <#assign timestamp = "" />
          <#if report.viewURLs[item_index]?exists>
            <#assign url = report.viewURLs[item_index] />
          </#if>
          <#if (report.linkCheck[item.URI])?exists>
            <#assign linkCheck = report.linkCheck[item.URI] />
            <#if linkCheck['brokenLinks']?exists>
              <#assign brokenLinks = linkCheck['brokenLinks'] />
            </#if>
            <#if linkCheck['timestamp']?exists>
              <#assign timestamp = linkCheck['timestamp'] />
            </#if>
          </#if>
          <#assign linkStatus = vrtx.propValue(item, 'link-status') />

          <#assign rowType = "odd" />
          <#if (item_index % 2 == 0) >
            <#assign rowType = "even" />
          </#if>

          <#assign firstLast = ""  />
          <#if (item_index == 0) && (item_index == (brokenLinksSize - 1))>
            <#assign firstLast = " first last" />
          <#elseif (item_index == 0)>
            <#assign firstLast = " first" />
          <#elseif (item_index == (brokenLinksSize - 1))>
            <#assign firstLast = " last" />     
          </#if>
          
          <#assign lastModified = vrtx.propValue(item, 'lastModified') />
          
          <#assign brokenLinksList>
            <#assign onlyCount = false />
            <#assign countedBrokenLinks = 0 />
            <#if brokenLinks?exists>
              <ul>
                <#list brokenLinks as link>
                  <#if link?is_hash>
                    <#if (link.link)?exists>
                      <#if (report.linkType == "anchor-img" &&  (link.type == "ANCHOR" || link.type == "IMG" || link.type == "PROPERTY"))
                        || (report.linkType == "anchor" && link.type == "ANCHOR")
                        || (report.linkType == "img"    && (link.type == "IMG" || link.type == "PROPERTY"))
                        || (report.linkType == "other"  && link.type != "ANCHOR" && link.type != "IMG" && link.type != "PROPERTY")>
                        <#assign countedBrokenLinks = countedBrokenLinks + 1 />
                        <#if !onlyCount>
                          <li>${link.link?html}<#if (link.status)?exists><!--${link.status?html}--></#if></li>
                        </#if>
                      </#if>
                    </#if>
                  <#else>
                    <#assign countedBrokenLinks = countedBrokenLinks + 1 />
                    <#if !onlyCount>
                      <li>${link?html}</li>
                    </#if>
                  </#if>
                  <#if (countedBrokenLinks > 9 && !onlyCount)>
                    <li>...</li>
                    <#assign onlyCount = true />
                  </#if>
                </#list>
              </ul>
            </#if>
          </#assign>
   
          <tr class="${rowType}${firstLast}">
            <td class="vrtx-report-broken-links-web-page">
              <a href="${url?html}">${title?html}</a>
              <span>${item.URI?html}</span>
            </td>
            <td class="vrtx-report-broken-links-count">
              ${countedBrokenLinks}
            </td>
            <td class="vrtx-report-broken-links">
              <#if brokenLinksList?exists>
                ${brokenLinksList}
              </#if>
            </td>
            <td class="vrtx-report-last-modified">
              <#if timestamp != "">
                <@vrtx.date value=timestamp?datetime('yyyyMMdd HH:mm:ss') format="long" />
              </#if>
              <#if timestamp != "" && linkStatus = 'AWAITING_LINKCHECK'>
                - 
              </#if>
              <#if linkStatus = 'AWAITING_LINKCHECK'>
                <@vrtx.msg code="report.broken-links.awaiting-linkcheck" />
              </#if>
            </td>
          </tr>
        </#list>
        </tbody>
      </table>
    </div>
    <#if report.prev?exists || report.next?exists>
      <p id="vrtx-report-paging-bottom">
        <@displayPaging />
      </p>
    </#if>
  <#else>
    <p id="vrtx-report-broken-links-info"><@vrtx.msg code="report.${report.reportname}.info.web-pages.none-found" />  ${linkTypeLocalization?lower_case}.<span class="vrtx-report-broken-links-info-number">&nbsp;</span></p>
  </#if>

  <#macro generateFilters filters>
    <#if filters?exists && (filters?size > 0)>
      <div id="vrtx-report-filters">
        <#list report.filters?keys as filterKey>
          <#local filterOpts = filters[filterKey] />
          <#if (filterOpts?size > 0)>
            <ul class="vrtx-report-filter" id="vrtx-report-filter-${filterKey}">
              <#list filterOpts as filterOpt>
                <#local filterID = "vrtx-report-filter-" + filterKey + "-" + filterOpt.name />
                <#if filterOpt.active>
                  <li class="active-filter" id="${filterID}">
                    <span><@vrtx.msg code="report.${report.reportname}.filters.${filterKey}.${filterOpt.name}" /></span>
                <#else>
                  <li id="${filterID}">
                    <a href="${filterOpt.URL?html}"><@vrtx.msg code="report.${report.reportname}.filters.${filterKey}.${filterOpt.name}" /></a>
                </#if>
                  </li>
              </#list>
            </ul>
          </#if>
        </#list>
      </div>
    </#if>
  </#macro>

  <#macro displayPaging>
    <span class="vrtx-report-paging">
      <#if report.prev?exists>
        <a href="${report.prev?html}" class="prev">
        <@vrtx.msg code="report.prev-page" default="Previous page" /></a><#if report.next?exists><a href="${report.next?html}" class="next"><@vrtx.msg code="report.next-page" default="Next page" /></a></#if>
      <#elseif report.next?exists>
        <a href="${report.next?html}" class="next"><@vrtx.msg code="report.next-page" default="Next page" /></a>
      </#if>
    </span>
  </#macro>
  
  </body>
</html>
