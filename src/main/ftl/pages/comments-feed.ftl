<?xml version="1.0" encoding="utf-8"?>
<#import "/lib/vortikal.ftl" as vrtx />
<rss version="2.0">
  <channel>
    <title>Comments for ${resource.title?html}</title>
    <link>${baseCommentURL?html}</link>
    <description>${resource.getPropertyByPrefix('content', 'description')?if_exists}</description>
    <pubDate><@vrtx.date value=resource.lastModified format='rfc-822' /></pubDate>
    <generator>vrtx</generator>

    <#assign comments = comments?sort_by("time") />
    <#list comments as comment>
    <item>
      <title>Comment by ${comment.author}</title>
      <link>${(baseCommentURL + '#comment-' + comment.ID?c)?html}</link>
      <guid>${(baseCommentURL + '#comment-' + comment.ID?c)?html}</guid>
      <author>${comment.author?html}</author>
      <pubDate><@vrtx.date value=comment.time format='rfc-822' /></pubDate>
      <description>${comment.content?html}</description>
    </item>
    </#list>

  </channel>
</rss>
