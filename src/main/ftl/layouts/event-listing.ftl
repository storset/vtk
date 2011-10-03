<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#if conf.includeIfEmpty>
  <#if psd?has_content>
    <div class="vrtx-event-component vrtx-event-component-psd">
  <#elseif res?has_content>
    <div class="vrtx-event-component vrtx-event-component-res">
  <#else>  
    <div class="vrtx-event-component">
  </#if>
      <#if conf.eventsTitle><h2><a href="${conf.uri?html}">${eventsTitle?html}</a></h2></#if>
      <#if psd?has_content>
        <#assign psdSize = psd?size />
        <#list psd as event>
          <@displayPsd event.ps event.date event.showTime event_index+1 psdSize />
        </#list>
      <#elseif res?has_content>
        <#assign resSize = res.files?size />
        <#list res.files as event>
          <@displayRes event event_index+1 resSize />
        </#list>
      <#else>
        <@vrtx.msg code="eventListing.noPlanned.allupcoming" />
      </#if>
      <#if conf.allEventsLink>
        <div class="vrtx-more">
          <span><a href="${conf.uri?html}"><@vrtx.msg code="event.go-to-events" default="Go to events" /></a></span>
        </div>
      </#if>
    </div>
</#if>

<#macro displayPsd event startdate showTime nr last>
  <#if nr == last>
    <div class="vrtx-event-component-item vrtx-event-component-item-${nr} last">
  <#else>
    <div class="vrtx-event-component-item vrtx-event-component-item-${nr}">
  </#if>
      <#local title = vrtx.propValue(event, 'title') />
      <#local location  = vrtx.propValue(event, 'location')  />
      <#local intro = vrtx.propValue(event, 'introduction')  />
      <#local introImg = vrtx.prop(event, 'picture')  />
      <#local caption = vrtx.propValue(event, 'caption')  />    

      <#if conf.dateIcon>
        <div class="vrtx-daily-events-date">
          <#local todayDay = vrtx.calcDate(today, 'dd') />
          <#local todayMonth = vrtx.calcDate(today, 'MM') />
          <#local tomorrowDay = vrtx.calcDate(tomorrow, 'dd') />
          <#local tomorrowMonth = vrtx.calcDate(tomorrow, 'MM') />
          <#local currentDay = vrtx.calcDate(startdate, 'dd') />
          <#local currentMonth = vrtx.calcDate(startdate, 'MM') />
          <#local todayLocalized = vrtx.getMsg("eventListing.calendar.today", "today") />
          <#local tomorrowLocalized = vrtx.getMsg("eventListing.calendar.tomorrow", "tomorrow") />

          <#if (vrtx.parseInt(currentDay) == vrtx.parseInt(todayDay)) && (vrtx.parseInt(currentMonth) == vrtx.parseInt(todayMonth)) >
            <span class="vrtx-daily-events-date-day vrtx-daily-events-date-today">${todayLocalized}</span>
          <#elseif (vrtx.parseInt(currentDay) == vrtx.parseInt(tomorrowDay)) && (vrtx.parseInt(currentMonth) == vrtx.parseInt(tomorrowMonth)) > 
            <span class="vrtx-daily-events-date-day vrtx-daily-events-date-tomorrow">${tomorrowLocalized}</span>
          <#else>
            <span class="vrtx-daily-events-date-day">${currentDay}</span>
          </#if>
          <span class="vrtx-daily-events-date-month"><@vrtx.date value=startdate format='MMM' /></span>
        </div>
      </#if>
      
      <div class="vrtx-event-component-main">
        <#if conf.showPicture && introImg?has_content>
          <#local captionFlattened>
            <@vrtx.flattenHtml value=caption escape=true />
          </#local>
          <div class="vrtx-event-component-picture">
            <#local introImgURI = vrtx.propValue(event, 'picture') />
            <#if introImgURI?exists>
              <#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
            <#else>
              <#local thumbnail = "" />
            </#if>
            <a class="vrtx-image" href="${event.URI?html}">
              <#if caption != ''>
                <img src="${thumbnail?html}" alt="${captionFlattened}" />
              <#else>
                <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
              </#if>
            </a>
          </div>
       </#if>

        <div class="vrtx-event-component-title">
          <a class="vrtx-event-component-title summary vrtx-link-check" href="${event.URI?html}">${title?html}</a>
        </div>

        <div class="vrtx-event-component-misc">
          <#if conf.dateIcon && showTime>
            <span class="vrtx-event-component-start-time">
              <@vrtx.date value=startdate format='HH:mm' /><#if conf.showLocation && (location != "")>,</#if>
            </span>
          <#elseif showTime>
            <span class="vrtx-event-component-start-time">
              <@vrtx.date value=startdate format='dd' />. 
              <@vrtx.date value=startdate format='MMM' />. 
              <@vrtx.date value=startdate format='yyyy' /> 
              <@vrtx.date value=startdate format='HH:mm' /><#if conf.showLocation && (location != "")>,</#if>
            </span>
          </#if>

          <#if conf.showLocation && (location != "")>
            <span class="vrtx-event-component-location location">${location}</span>
          </#if>

          <#if conf.addToCalendar>
            <div class="vrtx-number-of-comments-add-event-container">
              <span class="vrtx-add-event">
                <a class="vrtx-ical vrtx-link-check" href="${event.URI?html}?vrtx=ical"><@vrtx.msg code="event.add-to-calendar" /></a>
                <a class="vrtx-ical-help" href="${vrtx.getMsg("event.add-to-calendar.help-url")?html}" title="${vrtx.getMsg("event.add-to-calendar.help")?html}"></a>
              </span>
            </div> 
          </#if>
        </div>

        <#if conf.eventDescription>
          <div class="vrtx-event-component-introduction">
            ${intro}
          </div>
        </#if>
      </div>
    </div>
</#macro>

<#macro displayRes event nr last>
<#if nr == last>
  <div class="vrtx-event-component-item vrtx-event-component-item-${nr} last">
<#else>
  <div class="vrtx-event-component-item vrtx-event-component-item-${nr}">
</#if>
    <#local title = vrtx.propValue(event, 'title') />
    <#local location  = vrtx.propValue(event, 'location')  />
    <#local startDate = vrtx.propValue(event, 'start-date', 'long') />
    <#local startDateShort = vrtx.propValue(event, 'start-date', 'short') />
    <#local endDate = vrtx.propValue(event, 'end-date', 'long') />
    <#local endDateShort = vrtx.propValue(event, 'end-date', 'short') />
    <#local endDateTime = vrtx.propValue(event, 'end-date', 'hours-minutes') />
    <#local intro = vrtx.propValue(event, 'introduction')  />

    <div class="vrtx-event-component-title">
      <a class="vrtx-event-component-title summary vrtx-link-check" href="${event.URI?html}">${title?html}</a>
    </div>

    <#local startiso8601 = vrtx.propValue(event, "start-date", "iso-8601") />
    <#local endiso8601 = vrtx.propValue(event, "end-date", "iso-8601") />
    <#local isostarthour = "" />
    <#if startiso8601 != "" >
      <#local isostarthour = startiso8601?substring(11, 16) />
    </#if>
    <#local isoendhour = "" />
    <#if endiso8601 != "" >
      <#local isoendhour = endiso8601?substring(11, 16) />
    </#if>
    <#local isostartdate = "" />
    <#if startiso8601 != "" >
      <#local isostartdate = startiso8601?substring(0, 10) />
    </#if>
    <#local isoenddate = "" />
    <#if endiso8601 != "" >
      <#local isoenddate = endiso8601?substring(0, 10) />
    </#if>
    <div class="vrtx-event-component-misc">
      <span class="vrtx-event-component-start<#if conf.showEndTime>-and-end</#if>-time">
            <#if isostarthour != "00:00">${startDate}<#else>${startDateShort}</#if><#if conf.showEndTime && endiso8601 != ""><#if isoendhour != "00:00" &&  isostartdate != isoenddate> - ${endDate}<#elseif isostartdate != isoenddate> - ${endDateShort}<#elseif isoendhour != "00:00"> - ${endDateTime}</#if></#if><#if conf.showLocation && (location != "")>,</#if>
      </span>

      <#if conf.showLocation && (location != "")>
        <span class="vrtx-event-component-location">${location}</span>
      </#if>

      <#if conf.addToCalendar>
        <span class="vrtx-add-event">
          <a class="vrtx-ical vrtx-link-check" href="${event.URI?html}?vrtx=ical"><@vrtx.msg code="event.add-to-calendar" /></a><a class="vrtx-ical-help" href="${vrtx.getMsg("event.add-to-calendar.help-url")?html}" title="${vrtx.getMsg("event.add-to-calendar.help")?html}"></a>
        </span>
      </#if>
    </div>

    <#if conf.eventDescription>
      <div class="vrtx-event-component-introduction">
        ${intro}
      </div>
    </#if>
  </div>
</#macro>