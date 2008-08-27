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
<#macro displayImage resource>
  
  <#local imageRes = vrtx.propResource(resource, "picture") />
  <#local introductionImage = vrtx.propValue(resource, "picture") />
  
  <#if introductionImage != "">
    <#if imageRes == "">
      <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
    <#else>
      <#local userTitle = vrtx.propValue(imageRes, "userTitle", imageRes) />
      <#local desc = imageRes.getValueByName("description")?default("") />
      <#if userTitle == "" && desc == "">
        <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
      <#else>
        <#local pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
        <#local style="" />
        <#if pixelWidth != "">
          <#local style = "width:" + pixelWidth+ "px;" />
        </#if>
        <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
	      <#if userTitle != "">
	        <img src="${introductionImage}" alt="${userTitle?html}" />
	      <#else>
	        <img src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
	      </#if>
	      <div class="vrtx-imagetext">
	        <#if userTitle != "">
	          <span class="vrtx-imagetitle">${userTitle?html}<#if desc != "">: </#if></span>
	        </#if>
	        <#if desc != "">
	          <span class="vrtx-imagedescription">${desc?html}</span>
	        </#if>
	      </div> 
	    </div>
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
<#macro displayTimeAndPlace resource>

  <#local start = vrtx.propValue(resource, "start-date") />
  <#local startiso8601 = vrtx.propValue(resource, "start-date", "iso-8601") />
  <#local startshort = vrtx.propValue(resource, "start-date", "short") />
  <#local end = vrtx.propValue(resource, "end-date") />
  <#local endiso8601 = vrtx.propValue(resource, "end-date", "iso-8601") />
  <#local endshort = vrtx.propValue(resource, "end-date", "short") />
  <#local endhoursminutes = vrtx.propValue(resource, "end-date", "hours-minutes") />
  <#local location = vrtx.propValue(resource, "location") />
  
  <#local isoendhour = "" />
  <#if endiso8601 != "" >
    <#local isoendhour = endiso8601?substring(11, 16) />
  </#if>
  
  <#if start != "">
    <abbr class="dtstart" title="${startiso8601}">${start}</abbr><#rt />
  </#if>
  <#if end != "">
    <#if startshort == endshort>
      <#if isoendhour != "00:00">
        <#t /> - <abbr class="dtend" title="${endiso8601}">${endhoursminutes}</abbr><#rt />
      </#if>
    <#else>
      <#t /> - <abbr class="dtend" title="${endiso8601}">${end}</abbr><#rt />
    </#if>
  </#if>
  <#t /><#if location != "">, <span class="location">${location}</span></#if>
        
</#macro>