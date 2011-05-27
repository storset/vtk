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

<!-- begin feedback js -->
<script type="text/javascript" src="${url?html}"></script>
<!-- end feedback js -->

<a class="vrtx-feedback" href="${emailLink.url?html}" onClick="javascript:popup('${emailLink.url?html}'); return false"><@vrtx.msg code="decorating.feedbackComponent.emaillink" default="Feedback" /></a>