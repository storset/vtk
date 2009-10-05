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

<#assign resource = resourceContext.currentResource />

<#assign start = vrtx.propValue(resource, "start-date", "long", "resource") />
<#assign end = vrtx.propValue(resource, "end-date", "long", "resource") />
<#assign location = vrtx.propValue(resource, "location", "", "resource") />
<#assign title = vrtx.propValue(resource, "title", "", "resource") />

<#if start != "" || end != "" || location != "">
  <div class="vevent">
    <#t /><@viewutils.displayTimeAndPlace resource title "resource" "on" />
  </div>
</#if>