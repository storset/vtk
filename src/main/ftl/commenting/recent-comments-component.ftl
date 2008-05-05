<#ftl strip_whitespace=true />
<#import "/lib/vortikal.ftl" as vrtx />

<div class="vrtx-recent-comments">

  <a class="comments-title" href="${recentCommentsURL?html}"><@vrtx.msg code='commenting.comments.recent'
                   args=[resource.title] default='Recent comments' /></a>

  <#assign number = comments?size />

  <#-- XXX: -->
  <#if componentRequest['max-comments']?exists>
    <#attempt>
      <#assign number = vrtx.parseInt(componentRequest['max-comments']) />
    <#recover>
      <#stop "'max-comments' is not a number: " + componentRequest['max-comments'] />
    </#attempt>
  </#if>

  <#if number &lt; 0>
    <#stop "Number must be a positive integer" />
  </#if>

  <#if number &gt; comments?size>
    <#assign number = comments?size />
  </#if>


  <ul class="items">

  <#list comments as comment>
    <#if comment_index &gt; number - 1><#break /></#if>
    <li>
      <a class="item-title" href="${(commentURLMap[comment.ID] + '#comment-' + comment.ID)?html}">
      ${comment.author.description?html} <@vrtx.msg code="commenting.comments.on" default="on" />
        ${resourceMap[comment.URI].title?html}
      </a>
      <div class="item-description">
      <#assign description>
      <@vrtx.limit nchars=30 elide=true>
        <@vrtx.flattenHtml value=comment.content escape=false />
      </@vrtx.limit>
      </#assign>
      ${description?html}
      </div>
      <span class="published-date"><@vrtx.date value=comment.time format='long' /></span>
    </li>
  </#list>
  </ul>

  <a class="all-comments" href="${recentCommentsURL?html}"><@vrtx.msg code="commenting.comments.more" default="More..." /></a>
  
</div>

