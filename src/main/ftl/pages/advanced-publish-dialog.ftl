<#import "/lib/vortikal.ftl" as vrtx />
<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel 'resourceContext' missing">
</#if>

<#assign resource = resourceContext.currentResource />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
</head>
<body>
  <div>
  <div id="vrtx-advanced-publish-settings-dialog">
    <h1><@vrtx.msg code="publishing.advanced.title" /></h1>
    <#assign actionURL = vrtx.linkConstructor("", 'publishDialog') />
    <form method="post" action="${actionURL?html}">
      <fieldset>
       <label for="publishDate"><@vrtx.msg code="publishing.publish-date" default="Publish on" /> </label>
       <div class="vrtx-textfield">
         <@dateTimeInput "publish-date" "publishDate"/>
       </div>
       <label for="unpublishDate"><@vrtx.msg code="publishing.unpublish-date" default="Unpublish on" /></label>
       <div class="vrtx-textfield">
         <@dateTimeInput "unpublish-date" "unpublishDate" />
       </div>
       <@vrtx.csrfPreventionToken url=actionURL />
       <div id="submitButtons" class="submitButtons">
          <div class="vrtx-focus-button">
            <input type="submit" id="updateAction" name="updateAction" value="${vrtx.getMsg("editor.save")}"  />
          </div>
          <div class="vrtx-button">
            <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg("editor.cancel")}" />
          </div>
       </div>
      </fieldset>
    </form>
  </div>
  </div>
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