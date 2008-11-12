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
    <title><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" />: ${resource.title}</title>
 </head>
  <body>
    <h1><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" />: ${resource.title}</h1>   

    <#-- E-mail a friend form -->
    <div class="vrtx-email-a-friend-form">

     <form action="?vrtx=email-a-friend" method="post">
      <table border="0">
        <tbody>
            <tr>
                <td><label for="emailto"><@vrtx.msg code="tip.form.emailto" default="To: " /></label></td>
                <td><input type="text" name="emailto" id="emailto" /></td>
            </tr>
            <tr>
              <td colspan="2"><@vrtx.msg code="tip.form.emailtomessage" default="(use comma as a seperator if more than one e-mail recipient)" /></td> 
            </tr>
            <tr>
                <td><label for="emailfrom"><@vrtx.msg code="tip.form.emailfrom" default="From: " /></label></td>
                <td><input type="text" name="emailfrom" id="emailfrom" /></td>
            </tr>
            <tr>
              <td><label for="yourComment"><@vrtx.msg code="tip.form.yourcomment" default="Your comment: " /></label></td> 
              <td><textarea name="yourComment" id="yourComment"></textarea></td>
            <tr>
                <td><input type="submit" name="submit-tip" name="save" value="<@vrtx.msg code='tip.form.submit'
          default='Submit' />" /></td>
            </tr>    
        </tbody>
     </table>  
   </form>
 
     <#-- Postback from Controller -->
      <#if tipResponse?exists && tipResponse?has_content>
      <div id="tip-response"> 
           <#if tipResponse = "FAILURE-NULL-FORM">
               <@vrtx.msg code="tip.form.fail.null" default="You have to write something in both fields" />.
           <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
               <@vrtx.msg code="tip.form.fail.invalidate" default="One of the e-mailaddresses is invalid" />.
           <#elseif tipResponse = "FAILURE">
               <@vrtx.msg code="tip.form.fail.general" default="Tip was not sent" />.
           <#elseif tipResponse = "OK">
               <@vrtx.msg code="tip.form.success" default="Tip is sent to " /><#if emailSentTo?exists && emailSentTo?has_content> ${emailSentTo}</#if>.
        </#if> 
      </div>
      
      </#if>  
      <#-- If mail and thread exception -> show it to user -->
      <#if tipResponseMsg?exists && tipResponseMsg?has_content>
             <div><strong>${tipResponseMsg}</strong></div>
       </#if>
     </div>

</body>
</html>
