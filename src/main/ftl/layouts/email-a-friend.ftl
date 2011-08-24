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

<!-- begin email a friend js -->
<script type="text/javascript" src="${url?html}"></script>
<!-- end email a friend js -->

<a class="vrtx-email-friend" href="${emailLink.url?html}" onClick="javascript:popup('${emailLink.url?html}'); return false"><@vrtx.msg code="decorating.emailAFriendComponent.emaillink" default="E-mail a friend" /></a>