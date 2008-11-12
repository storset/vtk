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
    <title><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" /> - ${resource.title}</title>
    
    <style type="text/css">
      h2 {
        font-size: 120%; color: #333;
      }

      form#email-a-friend-form {
        width: 300px; text-align: left;
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
      
      form#email-a-friend-form div#tip-response {
        padding-top: 1em;
      }

    </style>

 </head>
  <body>
    <h1><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" /></h1>   
    <h2>${resource.title}</h2>

       <#-- E-mail a friend form -->
        <form id="email-a-friend-form" method="post" action="?vrtx=email-a-friend">
          <label for="emailTo"><@vrtx.msg code="tip.form.emailto" default="Send e-mail to" /></label> 
          <input type="text" id="emailTo" name="emailTo"/><div class="email-help"><@vrtx.msg code="tip.form.emailtomessage" default="Use comma as a seperator if more than one e-mail recipient" /></div> 
          <label for="emailFrom"><@vrtx.msg code="tip.form.emailfrom" default="Your e-mailadress" /></label> 
          <input type="text" id="emailFrom" name="emailFrom"/>
          <label for="yourComment"><@vrtx.msg code="tip.form.yourcomment" default="Your comment" /></label> 
          <textarea rows="3" cols="10" id="yourComment" name="yourComment"></textarea>
          <input type="submit" class="submit-email-form" value="Send" name="submit"/>
       </form>
       
            <#-- Postback from Controller -->
      <div id="tip-response"> 
         <#if tipResponse?exists && tipResponse?has_content>
           <#if tipResponse = "FAILURE-NULL-FORM">
               <@vrtx.msg code="tip.form.fail.null" default="You have to write something in both fields" />.
           <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
               <@vrtx.msg code="tip.form.fail.invalidate" default="One of the e-mailaddresses is invalid" />.
           <#elseif tipResponse = "FAILURE">
               <@vrtx.msg code="tip.form.fail.general" default="Tip was not sent" />.
           <#elseif tipResponse = "OK">
               <@vrtx.msg code="tip.form.success" default="Tip is sent to " /><#if emailSentTo?exists && emailSentTo?has_content> ${emailSentTo}</#if>
           </#if> 
        </#if>  
        
         <#-- If mail and thread exception -> show it to user -->
         <#if tipResponseMsg?exists && tipResponseMsg?has_content>
             <div><strong>${tipResponseMsg}</strong></p></div>
         </#if>
      </div>

       <#--  IP of sender
             <div id="tip-response-ip">
                 <#if senderIP?exists && senderIP?has_content>
                   <em>IP: ${senderIP}</em>
                 </#if>
            </div> -->

</body>
</html>
