<#-- -*- coding: utf-8 -*-
This template takes these parameters:

feed: a SyndFeed (from rome)

conf: RssConfig
containing:
  conf.includeLogo: boolean
  conf.includeDescription: boolean
  conf.includeUpdatedDate: boolean
  conf.includePublishedDate: boolean
  conf.maxMsgs: max number of messages to show
-->

<div class="feed">
    <h2 class="portlet-section-header">
      <a class="portlet-section-header" href="${feed.getLink()}">
	${feed.getTitle()}
      </a>
    </h2>

    <#-- logo -->
    <#if conf.includeLogo == true>
      <#if feed.getImage()?exists >
      <#assign image = feed.getImage()>
        <img class="rssLogo" alt="logo" src="${image.getUrl()}"/>
      </#if>
    </#if>
    
    <#if feed.getEntries()?exists >
    <ul class="portlet-setcion-body">
      <#assign maxMsgs = feed.getEntries()?size>
      <#if maxMsgs gt conf.maxMsgs>
      <#assign maxMsgs = conf.maxMsgs>
      </#if>      
      <#list feed.getEntries()[0..maxMsgs-1] as entry>
      <li class="title"> 
	<a href="${entry.getLink()}">
	  ${entry.getTitle()}
	</a>
	<ul>
	
	  <#-- description -->
	  <#if conf.includeDescription == true>
	  <#list entry.getContents() as content>
	  <li class="description">
	    ${content.getValue()}
	  </li>
	  </#list>
	  </#if>

	  <#-- published date -->
	  <#if conf.includePublishedDate == true>
	  <li class="published_date">
	    Publisert tidspunkt: 
	    <#if entry.getPublishedDate()?exists>
	    ${dateFormatter.formatDate(entry.getPublishedDate())}
	    </#if>
	  </li>
	  </#if>

	  <#-- updated date -->
	  <#if conf.includeUpdatedDate == true>
	  <li class="updated_date">
	    Sist oppdatert tidspunkt: 
	    <#if entry.getUpdatedDate()?exists>
	    ${dateFormatter.formatDate(entry.getUpdatedDate())}
	    </#if>
	  </li>
	  </#if>
	</ul>
      </li>
      </#list>
    </ul>
    </#if>

    <#if feed.getCopyright()?exists>
    <span class="portlet-font-dim">
      &copy; ${feed.getCopyright()}
    </span>
    </#if>
    
</div>
