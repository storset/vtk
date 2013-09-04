<#ftl strip_whitespace=true>
<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a link to the Email-A-Friend service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if !emailLink?exists || !emailLink.url?exists>
  <#stop "Missing 'emailLink' entry in model"/>
</#if>

<a class="vrtx-email-friend dialog" target="_blank" title='<@vrtx.msg code="tip.emailtitle" default="E-mail a friend" />' href="${emailLink.url?html}"><@vrtx.msg code="decorating.emailAFriendComponent.emaillink" default="E-mail a friend" /></a>