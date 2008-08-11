<#import "/lib/vortikal.ftl" as vrtx />

<#if !commentsEnabled?exists || !comments?exists || !title?exists>
  <#stop "Unable to render model: model data missing: required entries
          are 'commentsEnabled', 'comments', 'title'">
</#if>

<#if (commentsEnabled || comments?size &gt; 0) && feedURL?exists>
  <#assign linkTitle><@vrtx.msg code="commenting.comments" args=[title] /></#assign>
  <link type="application/atom+xml" rel="alternate" href="${feedURL?html}" title="${linkTitle}" />
</#if>
