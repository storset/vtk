<#import "/lib/vortikal.ftl" as vrtx />
<#if feed.entries?size &gt; 0 || conf.includeIfEmpty>
<div class="vrtx-feed">
  <#if conf.feedTitle?exists>
    <a class="feed-title" href="${feed.link}">${feed.title?html}</a> 
  <#elseif conf.feedTitleValue?exists>
    <div class="feed-title">${conf.feedTitleValue?html}</div> 
  </#if>

  <#if conf.feedDescription?exists>
    <div class="feed-description">${feed.description}</div> 
  </#if>
  
  <#if feed.entries?size gt 0>
    <#assign entries = feed.entries />
      <#if conf.sortByTitle?exists>
        <#assign entries = entries?sort_by("title") />
        <#if !conf.sortAscending?exists>
          <#-- Reverse order, descending sort requested, and ascending is default -->
          <#assign entries = entries?reverse />
        </#if> 
      <#else>
        <#if conf.sortAscending?exists>
          <#-- Feeds are by default sorted descending by publish-date, 
               reverse if ascending sort is requested -->
          <#assign entries = entries?reverse />
        </#if>
      </#if>

      <#assign maxMsgs = conf.maxMsgs />
      <#if entries?size lt maxMsgs>
        <#assign maxMsgs = entries?size />
      </#if>

     <ul class="items">
       <#list entries[0..maxMsgs-1] as entry>
         <li>
          <a class="item-title" href="${entry.link?html}">${entry.title?html}</a>
          <#if conf.publishedDate?exists && entry.publishedDate?exists>
          <span class="published-date">
            <@vrtx.date value=entry.publishedDate format="${conf.publishedDate}" />
          </span>
          </#if>
	      <#-- description -->
	      <#if conf.itemDescription?exists && (entry.description.value)?exists>
            <div class="item-description">
              ${entry.description.value?string}
            </div>
          </#if>
          <#if conf.displayCategories?exists && (entry.categories)?exists && (entry.categories)?size &gt; 0>
            <ul class="categories">
              <#list entry.categories as category>
                <li>${category.name}</li>
              </#list>
            </ul>
          </#if>
          <#if conf.displayChannel?exists>
            <#if conf.publishedDate?exists && entry.publishedDate?exists> - </#if><a href="${feedMapping.getUrl(entry)}" class="channel">${feedMapping.getTitle(entry)?html}</a> 
          </#if>
         </li>
      </#list>
    </ul>
  </#if>

  <#if conf.bottomLinkToAllMessages?exists>
  <a class="all-messages" href="${feed.link}">
   <@vrtx.msg code="decorating.feedComponent.allMessages" default="More..." />
  </a>
  </#if>
</div>
</#if>
