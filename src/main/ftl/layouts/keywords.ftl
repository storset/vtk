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

<#if value?exists || values?exists>
<span class="vrtx-tags">
  <#if title?exists>
    <span class="title">${title?html}:</span>
  <#else>
    <span class="title"><@vrtx.msg code="tags.title" default="Tags"/>:</span>
  </#if>
  <#if value?exists>
    <a href="${url?html}">${value?html}</a>
  <#else>
    <#list values as v>
      <a href="${urls[v_index]?html}">${v?html}</a><#if v_index &lt; values?size - 1>,<#t/></#if>
    </#list>
  </#if>
</span>
</#if>
