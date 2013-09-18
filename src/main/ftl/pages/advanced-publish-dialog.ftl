<#import "/lib/vortikal.ftl" as vrtx />
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel 'resourceContext' missing">
</#if>

<#assign resource = resourceContext.currentResource />

<html>
<head>
</head>
<body>
<h1><@vrtx.msg code="publishing.header" /></h1>
<form method="POST" action="${command.submitURL?html}">
      <div>
        <div>
            <@vrtx.msg code="publish.action.publish" default="Unpublish" /> 
         </div> 
         <div class="vrtx-textfield">
             <@dateTimeInput "publish-date" "publishDate"/>
         </div>
    </div>
    <div>
        <div>
            <@vrtx.msg code="publish.action.unpublish" default="Unpublish" /> 
        </div>
        <div class="vrtx-textfield">
            <@dateTimeInput "unpublish-date" "unpublishDate" />
        </div>
    </div>
    <div id="submitButtons" class="submitButtons">
          <div class="vrtx-focus-button">
            <input type="submit" id="updateAction" name="updateAction" value="${vrtx.getMsg("editor.save")}"  />
          </div>
          <div class="vrtx-button">
            <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
          </div>
     </div>
</form>
</body>
</html>

<#macro dateTimeInput propName id>
    <#local dateValue = vrtx.propValue(resource, propName, "iso-8601-short") />
    <#local timeValue = vrtx.propValue(resource, propName, "hours-minutes") />
    <#local dateTimeValue = "" />
    <#if dateValue?has_content && timeValue?has_content >
        <#local dateTimeValue = dateValue + " " + timeValue />
    </#if>
    <input class="date" type="text" id="${id}" name="${id}" value="${dateTimeValue}" />
</#macro>