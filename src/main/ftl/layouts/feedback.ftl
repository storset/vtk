<#ftl strip_whitespace=true>
<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a link to the Email-A-Friend service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if emailLink?exists && emailLink.url?exists && mailTo?exists>
  <@genFeedback emailLink.url url mailTo />
</#if>

<#macro genFeedback link jsUrl emailTo="" addFullUrl="" addPageTitle="">
  <!-- begin feedback js -->
  <script type="text/javascript" src="${jsUrl?html}"></script>
  <!-- end feedback js -->

  <div class="vrtx-feedback">
    <span class="vrtx-feedback-title">
      <span class="feedback-title"><@vrtx.msg code="feedback.could-not-find" default="Did you find what you were looking for?" /></span>
      <#if addFullUrl != "">
       <#local link = link?html + "&amp;fullurl=" + addFullUrl?url('UTF-8') />
        <#if addPageTitle != "">
          <#local link = link + "&amp;pagetitle=" + addPageTitle?url('UTF-8') />
        </#if>
        <#if emailTo != "">
          <#local link = link + "&amp;mailto=" + emailTo?url('UTF-8') />
        </#if>
        <a class="feedback" href="${link}" onclick="javascript:popup('${link}'); return false">
      <#else>
        <#if emailTo != "">
          <#local link = link + "&amp;mailto=" + emailTo?url('UTF-8') />
        </#if>
        <a class="feedback" href="${link?html}" onclick="javascript:popup('${link?html}'); return false">
      </#if>
          <@vrtx.msg code="feedback.title" default="Give feedback" />
        </a>
    </span>
    <span class="vrtx-feedback-bottom"></span>
  </div>
</#macro>