<#-- -*- coding: utf-8 -*-
This template takes these parameters:

feed: a SyndFeed (from rome)

conf: RssConfig
containing:
  conf.includeTitle: boolean
  conf.includeLogo: boolean
  conf.includeDescription: boolean
  conf.includeUpdatedDate: boolean
  conf.includePublishedDate: boolean
  conf.maxMsgs: max number of messages to show
-->

  <#if conf.includeTitle == true>
    <h2 class="feedTitle">
      <a href="${feed.getLink()}">${feed.getTitle()?html}</a>
    </h2>
  </#if>

    <#-- logo -->
    <#if conf.includeLogo == true>
      <#if feed.getImage()?exists >
      <#assign image = feed.getImage()>
        <img class="rssLogo" alt="logo" src="${image.getUrl()?html}"/>
      </#if>
    </#if>
    
    <#if feed.getEntries()?exists >
    <ul class="feedList">
      <#assign maxMsgs = feed.getEntries()?size>
      <#if maxMsgs gt conf.maxMsgs>
      <#assign maxMsgs = conf.maxMsgs>
      </#if>      
      <#list feed.getEntries()[0..maxMsgs-1] as entry>
      <li class="feedEntryTitle"> 
	<a href="${entry.getLink()?html}">
	  ${entry.getTitle()?html}
	</a>
        <#if conf.includeDescription == true ||
             conf.includePublishedDate == true || conf.includeUpdatedDate == true>
	<ul>
	
	  <#-- description -->
	  <#if conf.includeDescription == true>
	  <#list entry.getContents() as content>
	  <li class="feedDescription">
	    ${content.getValue()?html}
	  </li>
	  </#list>
	  </#if>

	  <#-- published date -->
	  <#if conf.includePublishedDate == true>
	  <li class="feedPublishedDate">
	    Published: 
	    <#if entry.getPublishedDate()?exists>
	    ${dateFormatter.formatDate(entry.getPublishedDate())}
	    </#if>
	  </li>
	  </#if>

	  <#-- updated date -->
	  <#if conf.includeUpdatedDate == true>
	  <li class="feedUpdatedDate">
	    Last updated:
	    <#if entry.getUpdatedDate()?exists>
	    ${dateFormatter.formatDate(entry.getUpdatedDate())}
	    </#if>
	  </li>
	  </#if>
	</ul>
        </#if>
      </li>
      </#list>
    </ul>
    </#if>

    <#--if feed.getCopyright()?exists>
    <span class="feedCopyright">
      &copy; ${feed.getCopyright()}
    </span>
    </#if-->
    

