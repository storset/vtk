<#import "/lib/vortikal.ftl" as vrtx />

<div class="vrtx-feed">
  <#if conf.feedTitle>
    <a class="feed-title" href="${feed.link}">${feed.title?html}</a> 
  </#if>

  <#if conf.feedDescription>
    <div class="feed-description">${feed.description?html}</div> 
  </#if>

  <#if feed.entries?exists>
    <#assign entries = feed.entries />
      <#if conf.sortByTitle>
        <#assign entries = entries?sort_by("title") />
      </#if>
      <#assign maxMsgs = feed.entries?size />
      <#if maxMsgs gt conf.maxMsgs>
        <#assign maxMsgs = conf.maxMsgs />
      </#if>      
     <ul class="items">
       <#list entries[0..maxMsgs-1] as entry>
         <li>
          <a class="item-title" href="${entry.link?html}">${entry.title?html}</a>
	  <#-- description -->
	  <#if conf.itemDescription>
          <div class="item-description">
            <#list entry.contents as content>
	       ${content.value} <#--${content.value?html}-->
	    </#list>
          </div>
        </#if>
        <#if conf.publishedDate?exists && entry.publishedDate?exists>
          <span class="published-date">
            <@vrtx.date value=entry.publishedDate format="${conf.publishedDate}" />
          </span>
        </#if>
      </li>
    </#list>
  </ul>
  </#if>

  <#if conf.bottomLinkToAllMessages>
  <a class="all-messages" href="${feed.link}">
   <@vrtx.msg code="decorating.feedComponent.allMessages" default="More" />
  </a>
  </#if>
</div>
