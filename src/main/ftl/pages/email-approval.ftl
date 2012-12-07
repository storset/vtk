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
  <title>${resource.title}</title>
  <meta name="robots" content="noindex"/> 
</head>
<body>
  <#if mailResponse?has_content && mailResponse = "OK">
     <p><@vrtx.msg code="email.form.success" args=[emailSentTo] /></p>
     <div class="vrtx-button">
       <button id="email-approval-success" onclick="javascript:vrtxSimpleDialogs.closeDialog('#dialog-html-send-approval');"><@vrtx.msg code="email.form.close" default="Close" /></button>
     </div>
  <#else>
    <#assign uri = vrtx.linkConstructor("", "emailApprovalService") />

    <form id="email-approval-form" method="post" action="${uri}">
      <@vrtx.csrfPreventionToken uri />
      
      <label for="emailTo" class="first"><@vrtx.msg code="email.form.to" default="Send e-mail to" /></label> 
      <div class="vrtx-textfield">  
        <#if emailSavedTo?has_content>
          <input type="text" id="emailTo" name="emailTo" value="${emailSavedTo?html}" />
        <#else>
          <input type="text" id="emailTo" name="emailTo" value="<#if editorialContacts??>${editorialContacts}</#if>" />
        </#if>
      </div>
      <div class="email-help"><@vrtx.msg code="email.form.to-tooltip" default="Use comma as a separator if sending to more than one e-mail recipient" /></div> 
      
      <#if userEmailFrom??>
        <label for="emailFrom"><@vrtx.msg code="email.form.from" default="Your e-mail address" /></label>  
        <div class="vrtx-textfield">  
          <#if emailSavedFrom?has_content>
            <input type="text" id="emailFrom" name="emailFrom" value="${emailSavedFrom?html}" />
          <#else>
            <input type="text" id="emailFrom" name="emailFrom" value="" />
          </#if>
        </div>
      </#if>
      
      <#if emailBody?has_content>
        <label><@vrtx.msg code="email.form.text" default="E-mail text" /></label>
        <div id="emailBody">
          <strong>${emailSubject}</strong>
          ${emailBody}
        </div>
      </#if>
       
      <label for="yourCommentTxtArea"><@vrtx.msg code="email.form.yourcomment" default="Your comment" /></label> 
      <#if yourSavedComment?has_content>
        <textarea class="round-corners" rows="6" cols="10" id="yourCommentTxtArea" name="yourComment">${yourSavedComment?html}</textarea>
      <#else>
        <textarea class="round-corners" rows="6" cols="10" id="yourCommentTxtArea" name="yourComment" value=""></textarea> 
      </#if>
      
      <div id="submitButtons">
        <div class="vrtx-focus-button"> 
          <input type="submit" class="submit-email-form" value="${vrtx.getMsg('send-to-approval.submit')}" name="submit" />
        </div>
        <div class="vrtx-button"> 
          <input type="button" onclick="javascript:vrtxSimpleDialogs.closeDialog('#dialog-html-send-approval');" class="cancel-email-form" value="${vrtx.getMsg('editor.cancel')}" name="cancel" />
        </div>  
      </div>
    </form>

    <#if mailResponse?has_content>
      <div id="email-response">
        <#if mailResponse = "failure-empty-fields">
          <span class="failure"><@vrtx.msg code="email.form.fail.null" default="One or more of the e-mail addresses are empty" />.</span>
        <#elseif mailResponse = "failure-invalid-emails">
          <span class="failure"><@vrtx.msg code="email.form.fail.invalidate" default="One of the e-mail addresses is not valid" />.</span>
        <#elseif mailResponse = "failure-general">
          <span class="failure"><@vrtx.msg code="email.form.fail.general" default="E-mail was not sent" /><#if mailResponseMsg?has_content>${mailResponseMsg}</#if>.</span>
        </#if> 
      </div>
    </#if>  
  </#if>
</body>
</html>
