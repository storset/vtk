<#ftl strip_whitespace=true>
<#--
  - File: email-a-friend.ftl
  - 
  - Description: Displays a email-a-friend form.
  -
  - Optional model data:
  -   form
  -   model
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title><@vrtx.msg code="feedback.title" default="Give feedback" /></title>
  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
      <link href="${cssUrl}" type="text/css" rel="stylesheet"/>
    </#list>
  </#if>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <script type="text/javascript">
    var thanksTitle = 
    $(function() {
      $(".submit-email-form").click(function() {
        var TBTitle = window.parent.document.getElementById("TB_ajaxWindowTitle");
        $(TBTitle).text('<@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" />');
      });
    });
  </script>
  <meta name="robots" content="noindex"/> 
 </head>
 <body>
    <h1><@vrtx.msg code="feedback.title" default="Give feedback" /></h1> 

    <!-- <p><@vrtx.msg code="feedback.cant-respond" default="We can unfortunately not respond directly." /></p>
    <p>
      <@vrtx.msg code="feedback.contact-pre" default="See" />&nbsp;
      <#if contacturl?has_content>
        <a id="vrtx-feedback-contact" target="_top" href='${contacturl?html}'>
      <#else>
        <a id="vrtx-feedback-contact" target="_top" href='<@vrtx.msg code="feedback.contact-link" default="http://www.uio.no/english/about/contact/" />'>
      </#if>
      <@vrtx.msg code="feedback.contact-middle" default="our points of contact" /></a>&nbsp;<@vrtx.msg code="feedback.contact-post" default="if you need answers from anyone." />
    </p> -->

     <#-- Feedback form -->
     <#if !like?exists || (like?exists && like = "false")>
       <form id="feedback-form" method="post" action="?vrtx=send-feedback">
         <#-- Your comment -->
         <label for="yourComment" style="display: none;"><@vrtx.msg code="feedback.form.yourcomment" default="Your comment" /></label> 
         <#if yourSavedComment?exists && yourSavedComment?has_content>
           <textarea rows="15" cols="10" id="yourComment" name="yourComment">${yourSavedComment?html}</textarea>
         <#else>
           <textarea rows="15" cols="10" id="yourComment" name="yourComment"></textarea> 
         </#if>
       
         <#if mailto?has_content>
           <input type="hidden" name="mailto" value="${mailto?html}" />
         </#if>
         
         <#if contacturl?has_content>
           <input type="hidden" name="contacturl" value="${contacturl?html}" />
         </#if>
       
         <input type="submit" class="submit-email-form" value="Send" name="submit"/>
      </form>
    </#if>
       
    <#-- Postback from Controller -->
    <div id="tip-response"> 
       <#if tipResponse?exists && tipResponse?has_content>
         <#if tipResponse = "FAILURE-NULL-FORM">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.null" default="You have to write a comment" />.</span>
         <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.invalidate" default="One of the e-mail addresses is invalid" />.</span>
         <#elseif tipResponse = "FAILURE">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.general" default="Feedback was not sent" /><#if tipResponseMsg?exists && tipResponseMsg?has_content>${tipResponseMsg}</#if>.</span>
         <#elseif tipResponse = "OK">
           <@vrtx.msg code="feedback.form.success" args=[emailSentTo] />
         </#if> 
      </#if>  
    </div>
</body>
</html>
