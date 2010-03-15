<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayEvents collection hideNumberOfComments=false displayMoreURLs=false >

  <#local displayType = vrtx.propValue(collection, 'display-type', '', 'el') />
  <#if !displayType?has_content && searchComponents?has_content>
    <#list searchComponents as searchComponent>
      <@displayStandard searchComponent hideNumberOfComments displayMoreURLs />
    </#list>
  <#elseif displayType = 'calendar'>
    <@displayCalendar hideNumberOfComments displayMoreURLs />
  </#if>
  
</#macro>>

<#macro displayStandard collectionListing hideNumberOfComments displayMoreURLs >
  
  <#local events = collectionListing.files />
  <#if events?size &gt; 0>

    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2> 
    </#if>
    <#list events as event>
      <@displayEvent collectionListing event hideNumberOfComments displayMoreURLs />
    </#list>
   </div>
  </#if>

</#macro>

<#macro displayCalendar hideNumberOfComments displayMoreURLs>
  
  <div id="vrtx-main-content">
	  <#if allUpcoming?has_content>
	    <@displayStandard allUpcoming hideNumberOfComments displayMoreURLs />  
	
	  <#elseif allPrevious?has_content>
	    <@displayStandard allPrevious hideNumberOfComments displayMoreURLs />
	
	  <#elseif specificDate?has_content && specificDate>
	    <#if specificDateEvents?has_content && specificDateEvents?size &gt; 0>
	      <h2 class="vrtx-events-specific-date">${specificDateEventsTitle}</h2>
	      <@displayStandard specificDateEvents hideNumberOfComments displayMoreURLs=false />
	    <#else>
	      <h3 class="vrtx-events-no-planned">${noPlannedEventsMsg}</h3>
	    </#if>
	
	  <#elseif groupedByDayEvents?has_content || furtherUpcoming?has_content>
	  
	    <#if groupedByDayEvents?has_content && groupedByDayEvents?size &gt; 0>
	      <div id="vrtx-daily-events">
	        <h2 class="vrtx-events-title">${groupedEventsTitle?html}</h2>
	      <#assign count = 1 />
	      <#list groupedByDayEvents as groupedEvents>
	        <div id="vrtx-daily-events-${count}" class="vrtx-daily-events-listing">
	          <div class="vrtx-daily-events-date">
	            <span class="vrtx-daily-events-date-day"><@vrtx.date value=groupedEvents.day format='dd' /></span>
	            <span class="vrtx-daily-events-date-month"><@vrtx.date value=groupedEvents.day format='MMM' /></span>
	          </div>
	          <div class="vrtx-daily-event">
	          <#local eventListing = groupedEvents.events /> 
	          <#list eventListing.files as event>
	            <@displayEvent eventListing event hideNumberOfComments displayMoreURLs=false />
	          </#list>
	          </div>      
	        </div>
	        <#assign count = count +1 />
	      </#list>
	      </div>
	    </#if>
	    
	    <#if furtherUpcoming?has_content && furtherUpcoming?size &gt; 0>
	      <h2 class="vrtx-events-further-upcoming">${furtherUpcomingTitle?html}</h2>
	      <@displayStandard furtherUpcoming hideNumberOfComments displayMoreURLs=false />
	    </#if>
	    
	    <div id="vrtx-events-nav">
	       <a href="${viewAllUpcomingURL}" id="vrtx-events-nav-all-upcoming"><@vrtx.msg code="eventListing.allUpcoming" default="Upcoming events"/></a>
	       <a href="${viewAllPreviousURL}" id="vrtx-events-nav-all-previous"><@vrtx.msg code="eventListing.allPrevious" default="Previous events"/></a>
	    </div>
	    
	  </#if>
  </div>
  
  <div id="vrtx-additional-content">
     <div id="vrtx-event-calendar">
       <#local clickableDayTitle = vrtx.getMsg("eventListing.calendar.dayHasPlannedEvents", "View upcoming events this day") />
       <#local notClickableDayTitle = vrtx.getMsg("eventListing.calendar.dayHasNoPlannedEvents", "No upcoming events this day") />
       <#local language = vrtx.getMsg("eventListing.calendar.lang", "en") />
       <script type="text/javascript">
       <!--
         $(document).ready(function() {
           eventListingCalendar('${plannedEventDatesServiceBaseUrl}', '${clickableDayTitle}', '${notClickableDayTitle}', '${language}');
         });
       // -->
       </script>
       <div type="text" id="datepicker"></div>
     </div>
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
      <#local src = vrtx.propValue(event, 'picture', 'thumbnail') />
      <a class="vrtx-image" href="${parent.urls[event.URI]?html}">
        <#if caption != ''>
          <img src="${src?html}" alt="${captionFlattened}" />
        <#else>
          <img src="${src?html}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
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
      <div class="description introduction">${intro.value}</div>
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