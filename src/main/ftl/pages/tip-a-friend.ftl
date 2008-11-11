<#ftl strip_whitespace=true>
<#--
  - File: tip-a-friend.ftl
  - 
  - Description: Displays a tip-a-friend form.
  -
  - Optional model data:
  -   form
  -   model
  -->
<#import "/lib/vortikal.ftl" as vrtx />

<div class="vrtx-tip-a-friend">

     <form action="?vrtx=tip-a-friend" method="post">
      <table border="0">
        <tbody>
            <tr>
                <td><label for="emailto"><@vrtx.msg code="tip.form.emailto" default="To: " /></label></td>
                <td><input type="text" name="emailto" id="emailto" /></td>
            </tr>
            <tr>
                <td><label for="emailfrom"><@vrtx.msg code="tip.form.emailfrom" default="From: " /></label></td>
                <td><input type="text" name="emailfrom" id="emailfrom" /></td>
            </tr>
            <tr>
                <td><input type="submit" name="submit-tip" name="save" value="<@vrtx.msg code='tip.form.submit'
          default='Submit' />" /></td>
            </tr>    
        </tbody>
     </table>  
   </form>
 
    <#if tipResponse?exists && tipResponse?has_content>
      <div id="tip-response"> 
       <#if tipResponse = "FAILURE-NULL-FORM">
               <@vrtx.msg code="tip.form.fail.null" default="You have to write something in both fields" />.
       <#elseif tipResponse = "FAILURE-INVALID-EMAIL">
               <@vrtx.msg code="tip.form.fail.invalidate" default="One of the e-mailaddresses is invalid" />.
       <#elseif tipResponse = "FAILURE">
               <@vrtx.msg code="tip.form.fail.general" default="Tip was not sent" />.
       <#elseif tipResponse = "OK">
               <@vrtx.msg code="tip.form.success" default="Tip is sent to " /> ${emailSentTo}.
        </#if> 
      </div>
    </#if>  

</div>