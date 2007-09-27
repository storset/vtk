<#ftl strip_whitespace=true>
<#--
  - File: ping.ftl
  - 
  - Description: see /lib/ping.ftl
  - 
  - Required model data:
  -  pingURL
  - Optional model data:
  -
  -->

<#import "/lib/ping.ftl" as ping />

<@ping.ping url=pingURL['url'] interval=600/>
