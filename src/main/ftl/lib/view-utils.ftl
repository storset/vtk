<#ftl strip_whitespace=true>
<#--
  - File: view-utils.ftl
  - 
  - Description: Utility macros for displaying content in views
  -   
  -->

<#import "vortikal.ftl" as vrtx />

<#--
 * Displays the introduction, if any, of a resource.
 * 
 * @param resource The resource to display introduction from
-->
<#macro displayIntroduction resource>
  <#local introduction = vrtx.propValue(resource, "introduction") />
  <#if introduction != "">
    <div class="vrtx-introduction">${introduction}</div>
  </#if>
</#macro>

<#--
 * Displays the image-property of a resource.
 * 
 * @param resource The resource to display image from
-->
<#macro displayImage resource displayAsThumbnail=false>
  
  <#local imageRes = vrtx.propResource(resource, "picture") />
  <#local introductionImage = vrtx.propValue(resource, "picture") />
  <#local caption = vrtx.propValue(resource, "caption") />
  
  <#-- Flattened caption for alt-tag in image -->
  <#local captionFlattened>
    <@vrtx.flattenHtml value=caption escape=true />
  </#local>
  
  <#if introductionImage != "">
  
    <#if imageRes == "">
      <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
    <#else>

      <#if displayAsThumbnail>
        <#local introductionImage = vrtx.propValue(resource, "picture", "thumbnail") />
      </#if>

      <#local pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
      <#local photographer = imageRes.getValueByName("photographer")?default("") />
        
      <#local style="" />
      <#if pixelWidth != "">
        <#local style = "width:" + pixelWidth+ "px;" />
      </#if>
        
      <#if caption != ""><#-- Caption is set -->
        <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
	      <img src="${introductionImage}" alt="${captionFlattened}" />
          <div class="vrtx-imagetext">
            <div class="vrtx-imagedescription">${caption}</div>
            <span class="vrtx-photo">
              <#if photographer != ""><#-- Image authors is set -->
                <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${photographer}
              </#if>
            </span>
          </div> 
        </div>
      <#else>
        <#if photographer != ""><#-- No caption but image author set -->
          <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
            <img src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />   
            <div class="vrtx-imagetext">
              <span class="vrtx-photo">
                <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${photographer}
              </span>
            </div>     
          </div>
        <#else><#-- No caption or image author set -->
          <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
        </#if>
	  </#if>
    </#if>
   </#if>
  
</#macro>


<#--
 * Shows the start- and end-date of an event, seperated by a "-".
 * If the two dates are identical, only the time of enddate is shown.
 * 
 * @param resource The resource to evaluate dates from
-->
<#macro displayTimeAndPlace resource title hideEndDate=false hideLocation=false hideNumberOfComments=false>
  
  <#local start = vrtx.propValue(resource, "start-date") />
  <#local startiso8601 = vrtx.propValue(resource, "start-date", "iso-8601") />
  <#local startshort = vrtx.propValue(resource, "start-date", "short") />
  <#local end = vrtx.propValue(resource, "end-date") />
  <#local endiso8601 = vrtx.propValue(resource, "end-date", "iso-8601") />
  <#local endshort = vrtx.propValue(resource, "end-date", "short") />
  <#local endhoursminutes = vrtx.propValue(resource, "end-date", "hours-minutes") />
  <#local location = vrtx.propValue(resource, "location") />
  <#local mapurl = vrtx.propValue(resource, "mapurl") />
  
  <#local isostarthour = "" />
  <#if startiso8601 != "" >
    <#local isostarthour = startiso8601?substring(11, 16) />
  </#if>
  
  <#local isoendhour = "" />
  <#if endiso8601 != "" >
    <#local isoendhour = endiso8601?substring(11, 16) />
  </#if>
  <#local locationMsgCode = "event.time" />
  <#if location != "" && !hideLocation><#t/>
    <#local locationMsgCode = "event.time-and-place" />
  </#if>
  <span class="time-and-place"><@vrtx.msg code=locationMsgCode />:</span>
  <span class="summary" style="display:none;">${title}</span>
  <#if start != "">
    <abbr class="dtstart" title="${startiso8601}">
    <#if isostarthour != "00:00">
      ${start}<#t/>
    <#else>
      ${startshort}<#t/>
    </#if>
    </abbr><#t/>
  </#if>
  <#if end != "" && !hideEndDate>
    <#if startshort == endshort>
      <#if isoendhour != "00:00">
        <#t /> - <abbr class="dtend" title="${endiso8601}">${endhoursminutes}</abbr><#rt />
      </#if>
    <#else>
      <#if start == "">
        (<@vrtx.msg code="event.ends" />) 
      <#else>
        - 
      </#if>
      <abbr class="dtend" title="${endiso8601}">
      <#if isoendhour != "00:00">
        ${end}<#t/>
      <#else>
        ${endshort}<#t/>
      </#if>
      </abbr><#t/>
    </#if>
  </#if>
  <#if location != "" && !hideLocation><#t/>,
    <span class="location">
    <#if mapurl == "">
      ${location}
    <#else>
      <a href="${mapurl?html}">${location}</a>
    </#if>
    </span>
  </#if>
  
  <#local constructor = "freemarker.template.utility.ObjectConstructor"?new() />
  <#local currentDate = constructor("java.util.Date") />
  <#local isValidStartDate = validateStartDate(resource, currentDate)?string == 'true' />
  
  <#local numberOfComments = vrtx.prop(resource, "numberOfComments") />
  <#if numberOfComments?has_content || isValidStartDate>	
    <div class="vrtx-number-of-comments-add-event-container">
  </#if>
  <#if !hideNumberOfComments >
    <#local locale = springMacroRequestContext.getLocale() />
    <@displayNumberOfComments resource locale />
  </#if>
  
  <#if isValidStartDate>
    <span class="vrtx-add-event">
      <#assign uri = vrtx.getUri(resource) />
      <a class="vrtx-ical" href="${uri?html}?vrtx=ical"><@vrtx.msg code="event.add-to-calendar" /></a><a class="vrtx-ical-help" href="${vrtx.getMsg("event.add-to-calendar.help-url")?html}" title="${vrtx.getMsg("event.add-to-calendar.help")?html}"></a>
    </span>
  </#if>
  
  <#if numberOfComments?has_content || isValidStartDate>
    </div>
  </#if>
	
</#macro>


<#--
 * Check the start date of an event to see if it's greater than the current date
 * Check if the event is "upcoming" and has a valid start date
 * 
 * @param event The resource to evaluate start date for
 * @param currentDate The date to compare the events start date to
-->
<#function validateStartDate event currentDate>
  <#local startDate = event.getPropertyByPrefix(nullArg, "start-date")?default("") />
  <#if !startDate?has_content>
    <#local startDate = event.getPropertyByPrefix('resource', "start-date")?default("") />
  </#if>
  <#if startDate != "">
    <#return startDate.getDateValue()?datetime &gt; currentDate?datetime />
  </#if>
  <#return "false"/>
</#function>

<#macro displayNumberOfComments resource locale  >
  <#local numberOfComments = vrtx.prop(resource, "numberOfComments") />
  <#if numberOfComments?has_content >
    <#assign uri = vrtx.getUri(resource) />
    <a href="${uri}#comments" class="vrtx-number-of-comments">
    <#if numberOfComments.intValue?number &gt; 1>
      <@vrtx.localizeMessage code="viewCollectionListing.numberOfComments" default="" args=[numberOfComments.intValue] locale=locale />
    <#else>
      <@vrtx.localizeMessage code="viewCollectionListing.numberOfCommentsSingle" default="" args=[] locale=locale />
    </#if>
    </a>
  </#if>
</#macro>

<#macro displayPageThroughUrls pageThroughUrls page >
  <#if pageThroughUrls?exists && (pageThroughUrls?size > 1) >
    <span class="vrtx-paging-wrapper">
    	<#list pageThroughUrls as url>
          <a href="${url.url?html}" class="<#if url.title == "prev">vrtx-previous<#elseif url.title="next">vrtx-next<#else>vrtx-page-number</#if><#if (url.marked) > vrtx-marked</#if>"><#compress>
          <#if (url.title) = "prev" ><@vrtx.msg code="viewCollectionListing.previous" />
          <#elseif (url.title) = "next" ><@vrtx.msg code="viewCollectionListing.next" />
          <#else >${(url.title?html)}
          </#if>       
          </#compress></a>
        </#list>
    </span>
  </#if>
</#macro>

<#macro displayShareSubNestedList titleI18n type>
  <div id="vrtx-${type}-component">
    <a href="javascript:void(0);" id="vrtx-${type}-link">${titleI18n}</a>
    <div id="vrtx-${type}-wrapper">
      <div id="vrtx-${type}-wrapper-inner">
        <div id="vrtx-${type}-top">
          <div id="vrtx-${type}-title">${titleI18n}</div>
          <span>
            <a href="javascript:void(0);" id="vrtx-${type}-close-link">
              <@vrtx.msg code="decorating.shareAtComponent.close" default="Close" />
            </a>
          </span>
        </div>
        <ul>
          <#nested>
        </ul>
      </div>
    </div>
  </div>
</#macro>