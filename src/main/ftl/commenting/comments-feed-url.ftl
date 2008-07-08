<#import "/lib/vortikal.ftl" as vrtx />

<#if !commentsEnabled?exists || !feedURL?exists || !title?exists>
  <#stop "Unable to render model: model data missing: required entries
          are 'commentsEnabled', 'feedURL' and 'title'">
</#if>

<#if commentsEnabled>
  <#assign linkTitle><@vrtx.msg code="commenting.comments" args=[title] /></#assign>
  <link type="application/atom+xml" rel="alternate" href="${feedURL?html}" title="${linkTitle}" />
</#if>
