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
  <title><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" /> - ${resource.title}</title>
  <#if cssURLs?exists>
    <#list cssURLs as cssUrl>
      <link href="${cssUrl}" type="text/css" rel="stylesheet" />
    </#list>
  </#if> 
  <#if (displayUpscoping?exists && displayUpscoping = "true") || resourceContext.repositoryId = "uio.no">
    <link href="/vrtx/__vrtx/static-resources/themes/default/forms.css" type="text/css" rel="stylesheet"/>
    <!--[if lte IE 7]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie7.css" type="text/css"/> 
    <![endif]--> 
    <!--[if lte IE 6]>
      <link rel="stylesheet" href="/vrtx/__vrtx/static-resources/themes/default/default-ie6.css" type="text/css"/> 
    <![endif]--> 
  </#if>
  <meta name="robots" content="noindex"/> 
 </head>
 <body>
   <#if tipResponse?has_content && tipResponse = "OK">
     <p><@vrtx.msg code="tip.form.success" args=[emailSentTo] /></p>
     <div class="vrtx-button">
       <button onclick="javascript:window.parent.tb_remove();"><@vrtx.msg code="tip.form.close" default="Close" /></button>
     </div>
   <#else>
    <h1><@vrtx.msg code="tip.emailtitle" default="E-mail a friend" /></h1>   
    <h2>${resource.title}</h2>

    <#-- E-mail a friend form -->
     <form id="email-a-friend-form" method="post" action="?vrtx=email-a-friend">
       
       <#-- Email to -->
       <label for="emailTo"><@vrtx.msg code="tip.form.emailto" default="Send e-mail to" /></label> 
       
       <div class="vrtx-textfield">  
         <#if emailSavedTo?exists && emailSavedTo?has_content>
           <input type="text" id="emailTo" name="emailTo" value="${emailSavedTo?html}"/>
         <#else>
           <input type="text" id="emailTo" name="emailTo" value=""/>
         </#if>
       </div>
       
       <div class="email-help"><@vrtx.msg code="tip.form.emailtomessage" default="Use comma as a separator if sending to more than one e-mail recipient" /></div> 
       
       <#-- Email from -->
       <label for="emailFrom"><@vrtx.msg code="tip.form.emailfrom" default="Your e-mail address" /></label>  
       
       <div class="vrtx-textfield">  
         <#if emailSavedFrom?exists && emailSavedFrom?has_content>
           <input type="text" id="emailFrom" name="emailFrom" value="${emailSavedFrom?html}"/>
         <#else>
           <input type="text" id="emailFrom" name="emailFrom" value=""/>
         </#if>
       </div>
       
       <#-- Your comment -->
       <label for="yourComment"><@vrtx.msg code="tip.form.yourcomment" default="Your comment" /></label> 
       
       <#if yourSavedComment?exists && yourSavedComment?has_content>
         <textarea class="round-corners" rows="6" cols="10" id="yourComment" name="yourComment">${yourSavedComment?html}</textarea>
       <#else>
         <textarea class="round-corners" rows="6" cols="10" id="yourComment" name="yourComment" value=""></textarea> 
       </#if>

       <div id="submitButtons">
         <div class="vrtx-focus-button"> 
           <input type="submit" class="submit-email-form" value="Send" name="submit" />
         </div>
         <div class="vrtx-button"> 
           <input type="button" onclick="javascript:window.parent.tb_remove();" class="cancel-email-form" value="Cancel" name="cancel" />
         </div>  
       </div>
    </form>
       
    <#-- Postback from Controller -->
    <#if tipResponse?has_content>
      <div id="tip-response">
      <#if tipResponse = "FAILURE-NULL-FORM">
        <span class="failure"><@vrtx.msg code="tip.form.fail.null" default="You have to write something in both fields" />.</span>
      <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
        <span class="failure"><@vrtx.msg code="tip.form.fail.invalidate" default="One of the e-mail addresses is invalid" />.</span>
      <#elseif tipResponse = "FAILURE">
        <span class="failure"><@vrtx.msg code="tip.form.fail.general" default="Tip was not sent" /><#if tipResponseMsg?exists && tipResponseMsg?has_content>${tipResponseMsg}</#if>.</span>
      </#if> 
      </div>
    </#if>  
  </#if>
</body>
</html>
