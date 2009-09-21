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

<#assign title = vrtx.propValue(resource, "title", "", "resource") />

<@viewutils.displayTimeAndPlace resource title "resource" />
