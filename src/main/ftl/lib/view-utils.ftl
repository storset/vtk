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
  <#local caption = vrtx.propValue(resource, "caption") />
  
  <#-- Flattened caption for alt-tag in image -->
  <#local captionFlattened>
        <@vrtx.flattenHtml value=caption escape=true />
  </#local>
  
  <#if introductionImage != "">
  
    <#if imageRes == "">
      <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
    <#else>
      <#if caption != "">
        <#local pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
        <#local style="" />
        <#if pixelWidth != "">
          <#local style = "width:" + pixelWidth+ "px;" />
        </#if>
        <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
	         <img src="${introductionImage}" alt="${captionFlattened}" />
            <div class="vrtx-imagetext">
                 <div class="vrtx-imagedescription">${caption}</div>
           </div> 
       </div>
      <#else>
        <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
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