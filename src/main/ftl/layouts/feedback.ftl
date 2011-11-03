<#ftl strip_whitespace=true>
<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a link to the Email-A-Friend service
  - 
  -->

<#import "/lib/vortikal.ftl" as vrtx/>

<#if emailLink?exists && emailLink.url?exists>
  <#if mailTo?exists>
    <@genFeedback emailLink.url url mailTo />
  <#else>
    <@genFeedback emailLink.url url />
  </#if>
</#if>

<#macro genFeedback link jsUrl emailTo="">
  <!-- begin feedback js -->
  <script type="text/javascript" src="${jsUrl?html}"></script>
  <script type="text/javascript"><!--
    $(function() {
      if (urchinTracker !== "undefined") {
        $(".feedback-yes").click(function() {  
          urchinTracker("/like" + document.location.pathname); 
        });
        $(".feedback-no").click(function() {
          urchinTracker("/dislike" + document.location.pathname);
        });
      }
    });
  // -->
  </script>
  <!-- end feedback js -->

  <div class="vrtx-feedback">
    <span class="vrtx-feedback-title">
      <span class="feedback-title"><@vrtx.msg code="feedback.title" default="Did you find what you were looking for?" /></span>
      <#if emailTo != "">
        <#local link = link + "&amp;mailto=" + emailTo?url('UTF-8') />
      </#if>
      <ul>
        <li>
          <a class="feedback-yes" href="${link?html}&amp;like=true" onclick="javascript:popup('${link?html}&amp;like=true'); return false">
            <@vrtx.msg code="feedback.link.yes" default="Yes I found it" />
          </a>
        </li>
        <li>
          <a class="feedback-no" href="${link?html}&amp;like=false" onclick="javascript:popup('${link?html}&amp;like=false'); return false">
            <@vrtx.msg code="feedback.link.no" default="No I did not find it" />
          </a>
        </li>
      </ul>
    </span>
    <span class="vrtx-feedback-bottom"></span>
  </div>
</#macro>