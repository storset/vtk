<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />

<#if thisMonth?exists && (ursTotal > 0)>
  <h3 class="resourceVisitHeader">
    <@vrtx.msg code="resource.metadata.about.visit" default="Visit count"/>
  </h3>
    
  <#assign months = [vrtx.getMsg("jan"), vrtx.getMsg("feb"), vrtx.getMsg("mar"), vrtx.getMsg("apr"), vrtx.getMsg("may"), vrtx.getMsg("jun"),
    vrtx.getMsg("jul"), vrtx.getMsg("aug"), vrtx.getMsg("sep"), vrtx.getMsg("oct"), vrtx.getMsg("nov"), vrtx.getMsg("dec")]>
    
  <#assign thisMonthBak = thisMonth>
  <#assign width = 360 + (20 * (ursNMonths + 1))>

  <ul>
    <li>
      <#list hosts as host><a href="${host?html}" onclick="javascript:getAJAXHtmlAsText('${host?html}', '#vrtx-resourceInfoMain', '#vrtx-resource-visit-wrapper'); return false;">${hostnames[host_index]?html}</a> </#list>
    </li>
  </ul>
  <div id="vrtx-resource-visit">
    <div id="vrtx-resource-visit-chart">
      <img id="vrtx-resource-visit-chart-image" width="${width}" height="225" alt="Visit chart"
           src="http://chart.apis.google.com/chart?chts=676767,15&amp;chxs=0,676767,13,0,l,676767|1,676767,13,0,l,676767&amp;chl=<#list 0..ursNMonths as i>${months[thisMonth]}<#if i != ursNMonths>|</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thisMonth = 11></#if></#list>&amp;chxr=0,28.333,${ursMonths[12]?string("0")}&amp;chxt=y,x&amp;chbh=a&amp;chs=${width}x225&amp;cht=bvg&amp;chco=ed1c24&amp;chds=0,${ursMonths[12]?string("0")}&amp;chd=t:<#assign thisMonth = thisMonthBak><#list 0..ursNMonths as i>${ursMonths[thisMonth]?string("0")}<#if i != ursNMonths>,</#if><#if thisMonth != 0><#assign thisMonth = thisMonth - 1><#else><#assign thisMonth = 11></#if></#list>&amp;chtt=<@vrtx.msg code="resource.metadata.about.visit.last${ursNMonths}months" />" />
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
    <span id="vrtx-resource-visit-info"><@vrtx.msg code="resource.metadata.about.visit.info" /> <a href="https://www.uio.no/for-ansatte/arbeidsstotte/nettpublisering/statistikk/">Urchin</a></span>
  </div>
</#if>