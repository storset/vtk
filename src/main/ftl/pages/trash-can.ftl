
<#import "/lib/vortikal.ftl" as vrtx />

<#if garbage?exists>
  <#-- Display garbage -->
<#else>
  <@vrtx.msg code="trash-can.empty" default="The trash can contains no garbage." />
</#if>