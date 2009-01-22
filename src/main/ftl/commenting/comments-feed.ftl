<?xml version="1.0" encoding="utf-8"?>
<#import "/lib/vortikal.ftl" as vrtx />
<feed xmlns="http://www.w3.org/2005/Atom">
  <title type="html">
    <#if resource.URI == '/'>
      <@vrtx.msg code='commenting.comments' args=[repositoryID] default='Comments' />
    <#else>
      <@vrtx.msg code='commenting.comments' args=[resource.title] default='Comments' />
    </#if>
  </title>
  <#assign uri = resource.URI.toString() />
  <link href="${urlMap[uri]?html}" />
  <link rel="self" href="${selfURL?html}" />
  <#assign date_format>yyyy-MM-dd'T'HH:mm:ssZZ</#assign>
  <updated><@vrtx.date value=resource.lastModified format=date_format/></updated>
  <id>${selfURL?html}</id>
  <#list comments as comment>
  <#assign resource = resourceMap[comment.URI] />
  <entry>
    <title>${comment.author.description?html} <@vrtx.msg code="commenting.comments.on" default="about" /> "${resource.title?html}"</title>
    <link href="${(urlMap[comment.URI] + '#comment-' + comment.ID)?html}" />
    <id>${(urlMap[comment.URI] + '#comment-' + comment.ID)?html}</id>
    <author>
      <name>${comment.author.description?html}</name>
      <#if comment.author.URL?exists>
      <uri>${comment.author.URL?html}</uri>
      </#if>
    </author>
    <published><@vrtx.date value=comment.time format=date_format /></published>
    <updated><@vrtx.date value=comment.time format=date_format /></updated>
    <summary type="html">${comment.content?html}</summary>
  </entry>
  </#list>
</feed>
