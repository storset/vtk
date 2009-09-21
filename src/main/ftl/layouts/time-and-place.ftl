<#ftl strip_whitespace=true>
<#--
  - File: time-and-place.ftl
  - 
  - Description: Event time and place
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />
<#-- Start-date, end-date, location and mapurl --> 

<#assign resource = resourceContext.currentResource />

<#-- TODO: figure out why these are not retrieved in resource. inheritance? -->
<#assign start = vrtx.propValue(resource, "start-date") />
<#assign end = vrtx.propValue(resource, "end-date") />
<#assign location = vrtx.propValue(resource, "location") />
<#assign mapurl = vrtx.propValue(resource, "mapurl") />
<#-- -->

<#assign title = vrtx.propValue(resource, "title") />

<#if start != "" || end != "" || location != "">
  <div class="vevent">
    <#t /><@viewutils.displayTimeAndPlace resource title />
  </div> 
</#if>