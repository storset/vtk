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

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>

    <title><@vrtx.msg code="feedback.title" default="Give feedback" /></title>
    
    <style type="text/css">
    /* Email-a-friend */

    form#email-a-friend-form {
      width: 310px; 
      text-align: left;
    }

    form#email-a-friend-form label {display: block;}
  
    form#email-a-friend-form input.submit-email-form {margin:0;}

    form#email-a-friend-form input#emailTo, 
    form#email-a-friend-form input#emailFrom {
      width: 100%;
      border: 1px solid #888;
      margin: 0.25em 0 1em 0; 
    }

    form#email-a-friend-form input#emailTo {
      margin-bottom: 0; 
    }
 
    form#email-a-friend-form textarea {
      margin: 0.25em 0 1em 0; 
      width: 100%;
      border: 1px solid #888;
    }

    form#email-a-friend-form div.email-help {
      margin-top: 5px;
      padding-top: 0em;
      font-size: 70%; 
      color: #888; 
      padding-bottom: 1em;
    }
      
    div#tip-response {
      margin-top: 1em;
      font-style: italic;
      font-size: 80%;
    }
    
    div#tip-response span.failure {
      color: #cc0000;
    }
    
    a#tip-close-link {
      display: block;
      font-size: 90%;
      float:right;
    }
    
    a#vrtx-feedback-contact {
      display: inline !important;
    }
    
    </style>
    
    <meta name="robots" content="noindex"/> 

 </head>
 <body>
    <a id="tip-close-link" href="javascript:window.close();"><@vrtx.msg code="tip.form.close" default="Close" /></a>
    <h1><@vrtx.msg code="feedback.title" default="Give feedback" /></h1>  
     
    <h2><@vrtx.msg code="feedback.thanks" default="Thank you for giving us feedback" /></h2>
    
    <p><@vrtx.msg code="feedback.cant-respond" default="We can unfortunately not respond directly." /></p>
    
    <p>
      <@vrtx.msg code="feedback.contact-pre" default="See" />&nbsp;<a id="vrtx-feedback-contact" href='<@vrtx.msg code="feedback.contact-link" default="http://www.uio.no/english/about/contact/" />'>
      <@vrtx.msg code="feedback.contact-middle" default="our points of contact" /></a>&nbsp;<@vrtx.msg code="feedback.contact-post" default="if you need answers from anyone." />
    </p>

     <#-- Feedback form -->
     <form id="email-a-friend-form" method="post" action="?vrtx=send-feedback">
       
       <#-- Your comment -->
       <label for="yourComment"><@vrtx.msg code="feedback.form.yourcomment" default="Your comment" /></label> 
       
       <#if yourSavedComment?exists && yourSavedComment?has_content>
         <textarea rows="9" cols="10" id="yourComment" name="yourComment">${yourSavedComment}</textarea>
       <#else>
         <textarea rows="9" cols="10" id="yourComment" name="yourComment" value=""></textarea> 
       </#if>
       
       <input type="submit" class="submit-email-form" value="Send" name="submit"/>
    </form>
       
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
