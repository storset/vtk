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

<hr />
<h3 class="uio-feedback"><@vrtx.msg code="feedback.could-not-find" default="Did you find what you were looking for?" />
  <a class="feedback" href="${emailLink.url?html}" onClick="javascript:popup('${emailLink.url?html}'); return false">
    <@vrtx.msg code="feedback.title" default="Give feedback" />
  </a>
</h3>
<hr />