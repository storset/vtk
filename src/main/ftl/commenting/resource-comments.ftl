<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
  <head>
    <#assign title>
      <#compress>
        <@vrtx.msg code='commenting.comments'
                   args=[resource.title] default='Comments' />
      </#compress>
    </#assign>
    <link type="application/atom+xml" rel="alternate" href="${feedURL?html}" title="${title?html}" />
    <#if cssURLs?exists>
      <#list cssURLs as cssUrl>
        <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
      </#list>
    </#if>
    <title>${title?html}</title>
  </head>
  <body>
    <h1>${title?html}</h1>
    <ul class="recent-comments">
    <#list comments as comment>
        <li><h2>
          <a href="${(commentURLMap[comment.ID] + '#comment-' + comment.ID)?html}">
            ${comment.author.description?html}
	    <@vrtx.msg code="commenting.comments.on" default="on" />
            ${resourceMap[comment.URI].title?html}
          </a></h2>
          <div class="comment">${comment.content}</div>
          <span class="pubdate"><@vrtx.date value=comment.time format='long' /></span>
        </li>
    </#list>
    </ul>
  </body>
</html>
