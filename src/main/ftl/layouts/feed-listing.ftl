<#import "/lib/vortikal.ftl" as vrtx />
<#if !conf.auth?exists && (feed.entries?size &gt; 0 || conf.includeIfEmpty)>
<div class="vrtx-feed<#if conf.itemPicture?exists > with-images</#if>">
  <#if overrideFeedTitle?exists && feed.link?exists>
  	<a class="feed-title" href="${feed.link}">${overrideFeedTitle}</a>
  <#elseif conf.feedTitle?exists && feed.link?exists>
    <a class="feed-title" href="${feed.link}">${feed.title?html}</a> 
  <#elseif conf.feedTitleValue?exists>
    <div class="feed-title">${conf.feedTitleValue?html}</div> 
  </#if>

  <#if conf.feedDescription?exists && feed.description?exists>
    <div class="feed-description">${feed.description}</div> 
  </#if>
  
  <#if feed.entries?size gt 0>
    <#assign entries = feed.entries />
      <#if conf.sortByTitle?exists>
        <#assign entries =entries?sort_by("title") />
        <#if !conf.sortAscending?exists>
          <#-- Reverse order, descending sort requested, and ascending is default -->
          <#assign entries = entries?reverse />
        </#if> 
      <#else>

        <#if (conf.sortDescending)?exists && conf.sortDescending>
          <#assign entries = entries?sort_by("publishedDate")?reverse />

        <#elseif (conf.sortAscending)?exists && conf.sortAscending>
          <#assign entries = entries?sort_by("publishedDate") />
        </#if>
      </#if>

      <#assign maxMsgs = conf.maxMsgs />
      <#if entries?size lt maxMsgs>
        <#assign maxMsgs = entries?size />
      </#if>

     <ul class="items">
       <#assign "counter" = 1>
       <#list entries[0..maxMsgs-1] as entry>
         <#if counter == maxMsgs>
           <li class="item-${counter} item-last">
         <#else>
		   <li class="item-${counter}">
		 </#if>
       	 <#list elementOrder as element >
         	<@displayEntry entry conf element />
         </#list>
         </li>
         <#assign counter = counter+1>
      </#list>
    </ul>
  </#if>
  
  <#if displayIfEmptyMessage?exists && feed.entries?size = 0>
	<span class="vrtx-empty-message">
	 	${displayIfEmptyMessage}
	 </span>
  </#if>

  <#if conf.bottomLinkToAllMessages?exists && feed.link?exists>
  <a class="all-messages" href="${feed.link}">
   <@vrtx.msg code="decorating.feedComponent.allMessages" default="More..." />
  </a>
  </#if>
</div>
</#if>

<#macro displayEntry entry conf element>
 <#if element = "title" >
     <a class="item-title" href="<#if entry.link?exists>${entry.link?html}<#else>${entry.uri?html}</#if>">${entry.title?trim?html}</a>
 </#if>
 <#if element = "publishDate" >
     <#if conf.publishedDate?exists && entry.publishedDate?exists>
     <span class="published-date">
       <@vrtx.date value=entry.publishedDate format="${conf.publishedDate}" />
     </span>
     </#if>
  </#if>

    <#if element = "categories" >
     <#if conf.displayCategories?exists && (entry.categories)?exists && (entry.categories)?size &gt; 0>
       <ul class="categories">
         <#list entry.categories as category>
           <li>${category.name}</li>
         </#list>
       </ul>
     </#if>
  </#if>
  
  <#if element = "channel" >
     <#if conf.displayChannel?exists>
       <#if conf.publishedDate?exists && entry.publishedDate?exists> - </#if><a href="${feedMapping.getUrl(entry)}" class="channel">${feedMapping.getTitle(entry)?html}</a> 
     </#if>
  </#if>
  
  <#if element = "description" && conf.itemDescription?exists && descriptionNoImage[entry]?exists && descriptionNoImage[entry]?has_content>
    <div class="item-description">
       ${descriptionNoImage[entry]?string}
    </div>
  </#if>
  
  <#if element = "picture" && conf.itemPicture?exists && imageMap[entry]?exists && imageMap[entry]?has_content >
     <a class="vrtx-image" href="<#if entry.link?exists>${entry.link?html}<#else>${entry.uri?html}</#if>">${imageMap[entry]?string}</a>
  </#if>

</#macro>
