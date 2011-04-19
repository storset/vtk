<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayEvents collection hideNumberOfComments=false displayMoreURLs=false >

  <#local displayType = vrtx.propValue(collection, 'display-type', '', 'el') />
  <#if !displayType?has_content && searchComponents?has_content>
    <#list searchComponents as searchComponent>
      <@displayStandard searchComponent hideNumberOfComments displayMoreURLs />
    </#list>
  <#elseif displayType = 'calendar'>
    <@displayCalendar collection hideNumberOfComments displayMoreURLs />
  </#if>
  
</#macro>

<#macro displayStandard collectionListing hideNumberOfComments displayMoreURLs showTitle=true>
  
  <#local events = collectionListing.files />
  <#if events?size &gt; 0>

    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0 && showTitle>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    <#assign count = 1 />
    <#list events as event>
      <#if events?size == count>
        <span id="vrtx-last-event">
      </#if>
      <@displayEvent collectionListing event hideNumberOfComments displayMoreURLs />
       <#if events?size == count>
        </span>
      </#if>
      <#assign count = count +1 />
    </#list>
   </div>
  </#if>

</#macro>

<#macro displayCalendar collection hideNumberOfComments displayMoreURLs>
  <#if groupedByDayEvents?has_content && groupedByDayEvents?size &gt; 0>
    <div id="vrtx-main-content" class="vrtx-calendar-listing">
  <#else>
    <div id="vrtx-main-content" class="vrtx-calendar-listing vrtx-no-daily-events">
  </#if>
  <#if allupcoming?has_content>
	<h1>${allupcomingTitle?html}</h1>
	<#if allupcoming.files?size &gt; 0 >
	  <@displayStandard allupcoming hideNumberOfComments displayMoreURLs false />
	<#else>
	  <p class="vrtx-events-no-planned">${allupcomingNoPlannedTitle?html}</p>
	</#if>
  <#elseif allprevious?has_content>
    <h1>${allpreviousTitle?html}</h1>
    <#if allprevious.files?size &gt; 0 >
      <@displayStandard allprevious hideNumberOfComments displayMoreURLs false />
	<#else>
	  <p class="vrtx-events-no-planned">${allpreviousNoPlannedTitle?html}</p>
	</#if>
  <#elseif specificDate?has_content && specificDate>
    <h1 class="vrtx-events-specific-date">${specificDateEventsTitle?html}</h1>
    <#if specificDateEvents?has_content && specificDateEvents.files?size &gt; 0>
      <@displayStandard specificDateEvents hideNumberOfComments displayMoreURLs=false />
    <#else>
      <p class="vrtx-events-no-planned">${noPlannedEventsMsg?html}</p>
    </#if>
  <#elseif groupedByDayEvents?has_content || furtherUpcoming?has_content>
    <div class="vrtx-events-calendar-introduction">
    <#local title = vrtx.propValue(collection, "title", "flattened") />
    <h1>${title}</h1>
    <#local introduction = vrtx.getIntroduction(collection) />
    <#local introductionImage = vrtx.propValue(collection, "picture") />
    <#if introduction?has_content || introductionImage != "">
      <div class="vrtx-introduction">
        <#-- Image -->
        <@viewutils.displayImage collection />
        <#-- Introduction -->
        <#if introduction?has_content>
          ${introduction}
        </#if>
      </div>
    </#if>
    </div>
    <#if groupedByDayEvents?has_content && groupedByDayEvents?size &gt; 0>
      <div id="vrtx-daily-events">
        <h2 class="vrtx-events-title">${groupedEventsTitle?html}</h2>
      <#assign count = 1 />
      <#list groupedByDayEvents as groupedEvents>
        <div id="vrtx-daily-events-${count}" class="vrtx-daily-events-listing">
          <div class="vrtx-daily-events-date">
            <#local todayDay = vrtx.calcDate(today, 'dd') />
            <#local todayMonth = vrtx.calcDate(today, 'MM') />
            <#local tomorrowDay = vrtx.calcDate(tomorrow, 'dd') />
            <#local tomorrowMonth = vrtx.calcDate(tomorrow, 'MM') />
            <#local currentDay = vrtx.calcDate(groupedEvents.day, 'dd') />
            <#local currentMonth = vrtx.calcDate(groupedEvents.day, 'MM') />
            
            <#local todayLocalized = vrtx.getMsg("eventListing.calendar.today", "today") />
            <#local tomorrowLocalized = vrtx.getMsg("eventListing.calendar.tomorrow", "tomorrow") />
            
            <#if (vrtx.parseInt(currentDay) == vrtx.parseInt(todayDay)) 
              && (vrtx.parseInt(currentMonth) == vrtx.parseInt(todayMonth)) >
              <span class="vrtx-daily-events-date-day" id="vrtx-daily-events-date-today">${todayLocalized}</span>
            <#elseif (vrtx.parseInt(currentDay) == vrtx.parseInt(tomorrowDay)) 
                  && (vrtx.parseInt(currentMonth) == vrtx.parseInt(tomorrowMonth)) > 
              <span class="vrtx-daily-events-date-day" id="vrtx-daily-events-date-tomorrow">${tomorrowLocalized}</span>
            <#else>
              <span class="vrtx-daily-events-date-day">${currentDay}</span>
            </#if>
            
            <span class="vrtx-daily-events-date-month"><@vrtx.date value=groupedEvents.day format='MMM' /></span>
          </div>
          <div class="vrtx-daily-event">
            <#local eventListing = groupedEvents.events />
            <#assign subcount = 1 />
            <#list eventListing.files as event>
              <#if groupedByDayEvents?size == count && eventListing.files?size == subcount>
                <span id="vrtx-last-daily-event">
              </#if>
              <@displayEvent eventListing event hideNumberOfComments displayMoreURLs=false />
              <#if groupedByDayEvents?size == count && eventListing.files?size == subcount>
                </span>
              </#if>
	          <#assign subcount = subcount +1 />
	        </#list>
	      </div>
	    </div>
	    <#assign count = count +1 />
	  </#list>
	  </div>
    </#if>

    <#if furtherUpcoming?has_content && furtherUpcoming.files?size &gt; 0>
      <div class="vrtx-events-further-upcoming">
        <h2 class="vrtx-events-further-upcoming">${furtherUpcomingTitle?html}</h2>
        <@displayStandard furtherUpcoming hideNumberOfComments displayMoreURLs=false />
      </div>
    </#if>
    
    <#if furtherUpcoming?has_content && furtherUpcoming.files?size &gt; 0>
      <div id="vrtx-events-nav">
    <#else>
      <div id="vrtx-events-nav" class="vrtx-events-nav-top-border">
    </#if>
      <a href="${viewAllUpcomingURL}" id="vrtx-events-nav-all-upcoming">${viewAllUpcomingTitle?html}</a>
      <a href="${viewAllPreviousURL}" id="vrtx-events-nav-all-previous">${viewAllPreviousTitle?html}</a>
    </div>

  </#if>
  </div>

  <div id="vrtx-additional-content">
    <div id="vrtx-event-calendar">
      <#local activeDate = "" />
      <#if requestedDate?exists && requestedDate?has_content>
        <#local activeDate = requestedDate />
      </#if>
      <#local language = vrtx.getMsg("eventListing.calendar.lang", "en") />
      <script type="text/javascript">
      <!--
        $(document).ready(function() {
          eventListingCalendar(${allowedDates}, '${activeDate}', '${dayHasPlannedEventsTitle}', '${dayHasNoPlannedEventsTitle}', '${language}');
        });
      // -->
      </script>
      <div type="text" id="datepicker"></div>
    </div>
    <#assign additionalContent = vrtx.propValue(collection, "additionalContents") />
    <#if additionalContent?has_content>
	  <div id="vrtx-related-content">
        <@vrtx.invokeComponentRefs additionalContent />
	  </div>
	</#if>
  </div>

</#macro>

<#macro displayEvent parent event hideNumberOfComments displayMoreURLs >

  <#local locale = springMacroRequestContext.getLocale() />
  
  <#local title = vrtx.propValue(event, 'title') />
  <#local introImg = vrtx.prop(event, 'picture')  />
  <#local intro = vrtx.prop(event, 'introduction')  />
  <#local location  = vrtx.prop(event, 'location')  />
  <#local caption = vrtx.propValue(event, 'caption')  />
  <#local endDate = vrtx.prop(event, 'end-date') />
  <#local hideEndDate = !endDate?has_content || !parent.hasDisplayPropDef(endDate.definition.name) />
  <#local hideLocation = !location?has_content || !parent.hasDisplayPropDef(location.definition.name) />
 
  <#-- Flattened caption for alt-tag in image -->
  <#local captionFlattened>
    <@vrtx.flattenHtml value=caption escape=true />
  </#local>
  <div class="vrtx-resource vevent">
    <#if introImg?has_content && parent.hasDisplayPropDef(introImg.definition.name)>
      <#local introImgURI = vrtx.propValue(event, 'picture') />
      <#if introImgURI?exists>
			<#local thumbnail =  vrtx.relativeLinkConstructor(introImgURI, 'displayThumbnailService') />
	  <#else>
			<#local thumbnail = "" />
	  </#if>
      <a class="vrtx-image" href="${parent.urls[event.URI]?html}">
        <#if caption != ''>
          <img src="${thumbnail?html}" alt="${captionFlattened}" />
        <#else>
          <img src="${thumbnail?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
        </#if>
      </a>
    </#if>
    <div class="vrtx-title">
      <a class="vrtx-title summary" href="${parent.urls[event.URI]?html}">${title?html}</a>
    </div>

    <div class="time-and-place">
      <@viewutils.displayTimeAndPlace event title hideEndDate hideLocation hideNumberOfComments />
    </div>

    <#if intro?has_content && parent.hasDisplayPropDef(intro.definition.name)>
      <div class="description introduction">
      		<@vrtx.linkResolveFilter intro.value parent.urls[event.URI] requestURL />
      </div>
    </#if>

    <#local hasBody = vrtx.propValue(event, 'hasBodyContent') == 'true' />
    <#if displayMoreURLs && hasBody>
      <div class="vrtx-read-more">
        <a href="${parent.urls[event.URI]?html}" class="more" title="${title?html}">
          <@vrtx.msg code="viewCollectionListing.readMore" />
        </a>
      </div>
    </#if>
  </div>

</#macro>
