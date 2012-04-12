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
      <link href="${cssUrl}" type="text/css" rel="stylesheet" />
    </#list>
  </#if>
  <#if (displayUpscoping?exists && displayUpscoping = "true") || resourceContext.repositoryId = "uio.no">
    <link href="/vrtx/__vrtx/static-resources/themes/default/forms.css" type="text/css" rel="stylesheet" />
    <!--[if lte IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css" /> 
    <![endif]--> 
    <!--[if lte IE 6]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css" /> 
    <![endif]--> 
  </#if>
  <#if jsURLs?exists>
    <#list jsURLs as jsURL>
      <script type="text/javascript" src="${jsURL}"></script>
    </#list>
  </#if>
  <meta name="robots" content="noindex"/> 
 </head>
 <body>
    <#if tipResponse?has_content && tipResponse = "OK">
      <p><@vrtx.msg code="feedback.form.success" args=[emailSentTo] /></p>
      <div class="vrtx-button">
        <button onclick="javascript:window.parent.tb_remove();"><@vrtx.msg code="tip.form.close" default="Close" /></button>
      </div>
      <script type="text/javascript"><!--
        $(function() {
          var TBTitle = window.parent.document.getElementById("TB_ajaxWindowTitle");
          $(TBTitle).text('<@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" />');
        });
      // -->
      </script>
    <#else>
      <h1><@vrtx.msg code="feedback.title" default="Give feedback" /></h1> 

      <p><@vrtx.msg code="feedback.cant-respond" default="We can unfortunately not respond directly." /></p>
      <p>
      <@vrtx.msg code="feedback.contact-pre" default="See" />
      <#if contacturl?has_content>
        <a id="vrtx-feedback-contact" target="_top" href='${contacturl?html}'>
      <#else>
        <a id="vrtx-feedback-contact" target="_top" href='<@vrtx.msg code="feedback.contact-link" default="http://www.uio.no/english/about/contact/" />'>
      </#if>
      <@vrtx.msg code="feedback.contact-middle" default="our points of contact" /></a>&nbsp;<@vrtx.msg code="feedback.contact-post" default="if you need answers from anyone." />
      </p>

       <#-- Feedback form -->
       <#if !like?exists || (like?exists && like = "false")>
         <form id="feedback-form" method="post" action="?vrtx=send-feedback">
           <#-- Your comment -->
           <label for="yourComment" style="display: none;"><@vrtx.msg code="feedback.form.yourcomment" default="Your comment" /></label> 
           <#if yourSavedComment?exists && yourSavedComment?has_content>
             <textarea class="round-corners" rows="15" cols="10" id="yourComment" name="yourComment">${yourSavedComment?html}</textarea>
           <#else>
             <textarea class="round-corners" rows="15" cols="10" id="yourComment" name="yourComment"></textarea> 
           </#if>
       
           <#if mailto?has_content>
             <input type="hidden" name="mailto" value="${mailto?html}" />
           </#if>
         
           <#if contacturl?has_content>
             <input type="hidden" name="contacturl" value="${contacturl?html}" />
           </#if>
       
           <div id="submitButtons">
             <div class="vrtx-focus-button"> 
               <input type="submit" class="submit-email-form" value="Send" name="submit" />
             </div>
             <div class="vrtx-button"> 
               <input type="button" class="cancel-email-form" value="${vrtx.getMsg('editor.cancel')}" name="cancel" onclick="javascript:window.parent.tb_remove();" />
             </div>  
           </div>
         </form>
       </#if>
       
       <#-- Postback from Controller -->
       
       <#if tipResponse?has_content>
         <div id="tip-response"> 
           <#if tipResponse = "FAILURE-NULL-FORM">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.null" default="You have to write a comment" />.</span>
           <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.invalidate" default="One of the e-mail addresses is invalid" />.</span>
           <#elseif tipResponse = "FAILURE">
             <span class="failure"><@vrtx.msg code="feedback.form.fail.general" default="Feedback was not sent" /><#if tipResponseMsg?exists && tipResponseMsg?has_content>${tipResponseMsg}</#if>.</span>
           </#if>
         </div>
       </#if>
    </#if>
</body>
</html>
