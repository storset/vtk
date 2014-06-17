<#ftl strip_whitespace=true>

<@generateType result "plenary" />
<@generateType result "group" />

<#macro generateType result type>
  <#local sequences = {} />
  <#list result[type].activities as activity>
    <#local dtShort = activity.teachingMethod?lower_case />
    <#local dtLong = activity.teachingMethodName />
    <#local isFor = dtShort == "for" />
    
    <#local fixedResources = [] />
    <#local sessions = [] />
    <#list activity.sequences as sequence>
      <#local sessions = sessions + sequence.sessions />
      <#if sequence.vrtxResourcesFixed?exists>
        
      </#if>
    </#list>
    <h3>${dtLong}</h3>
    <#list sessions?sort_by("dtStart") as session>
      <p>${session.dtStart?string("yyyy-MM-dd HH:mm:ss zzzz")}</p>
    </#list>
  </#list>
</#macro>