<#import "/lib/vortikal.ftl" as vrtx />

<div class="vrtxFeed">
  <#if conf.feedTitle>
    <a class="feedTitle" href="${feed.link}">${feed.title?html}</a> <#-- vanskelig å si hvilken heading dette skal være hvis h1, h2, osv. -->
 </#if>

  <#if conf.feedDescription>
 <div class="feedDescription">${feed.description?html}</div> 
 </#if>

  <#if feed.entries?exists>
    <#assign entries = feed.entries>
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
        <a class="itemTitle" href="${entry.link?html}">${entry.title?html}</a>
	<#-- description -->
	<#if conf.itemDescription>
        <div class="itemDescription">
	
          <#list entry.contents as content>
	    <li class="feedDescription">
	      ${content.value}<#--${content.value?html}-->
	    </li>
	  </#list>
        </div><#-- fordi <p> ol. kan foekomme ... -->
        </#if>
        <#if conf.publishedDate && entry.publishedDate?exists>
          <span class="publishedDate">${entry.publishedDate?string(conf.format)}</span><#-- hva med bruk av skilletegn? -->
        </#if>
      </li>
    </#list>
  </ul>
  </#if>

  <#if conf.bottomLinkToAllMessages>
  <a class="all-messages" href="${feed.link}">
   <@vrtx.msg code="decorating.feedComponent.allMessages" default="All messages" />
  </a>
  </#if>
</div>
