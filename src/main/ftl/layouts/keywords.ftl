<#--
  - File: keywords.ftl
  - 
  - Description: displays the single or list of links
  - 
  - Required model data:
  -   either:
  -     url - a url
  -     value - a value
  -   or:
  -     urls - a list of urls
  -     values - a list of values
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if !value?exists && !values?exists>
  <#stop "Unable to render model: required model data
  'value' or 'values' missing">
</#if>

<#if value?exists>
  <a href="${url}">${value}</a>
<#else>
  <ul>
  <#list values as v>
    <li><a href="${urls[v_index]}">${v}</a></li>
  </#list>
</#if>
