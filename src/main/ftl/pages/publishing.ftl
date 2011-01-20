<#ftl strip_whitespace=true>

<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/propertyList.ftl" as propList />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required submodel 'resourceContext' missing">
</#if>

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Publishing status on document</title>
    <script type="text/javascript" src="${webResources?html}/jquery-ui-1.8.8.custom/js/jquery-ui-1.8.8.custom.min.js"></script>
    <script type="text/javascript" src="${webResources?html}/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-no.js"></script>
    <script type="text/javascript" src="${webResources?html}/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-nn.js"></script>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker-admin.js"></script>
  
    <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />

    <script type="text/javascript">
    <!--
      $(document).ready(function() {
          initDatePicker("${language}");
       });
    //-->
    </script>
    
    <link type="text/css" href="${webResources?html}/jquery-ui-1.8.8.custom/css/smoothness/jquery-ui-1.8.8.custom.css" rel="stylesheet" />
    <#if cssURLs?exists>
      <#list cssURLs as cssURL>
        <link rel="stylesheet" href="${cssURL}" />
      </#list>
    </#if>
  </head>
  <#assign resource = resourceContext.currentResource />
  <#assign header = vrtx.getMsg("publishing.header", "Publishing status on document") />
  <body>
    <div class="resourceInfo publishing">
      <h2>
        ${header}
      </h2>
        <#assign isPublished = resource.isPublished() />
        <#assign publishedStatusMsgKey = "publishing.status." + isPublished?string />
        <h3><@vrtx.msg code="publishing.status" default="Status" /></h3>
        <div>
        <@vrtx.msg code=publishedStatusMsgKey default="" />
        <#if isPublished>
          <#assign titleMsg = vrtx.getMsg("confirm-publish.title.unpublish") />
        (&nbsp;<a href="${unPublishUrl?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" class="thickbox" title="${titleMsg}"><@vrtx.msg code="publish.action.unpublish" default="unpublish" /></a>&nbsp;)
        <#else>
          <#assign titleMsg = vrtx.getMsg("confirm-publish.title.publish") />
        (&nbsp;<a href="${publishUrl?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" class="thickbox" title="${titleMsg}"><@vrtx.msg code="publish.action.publish" default="publish" /></a>&nbsp;)
        </#if>
        </div>
        <@displayOrEdit "publish-date" "publishDate" editPublishDateUrl />
        <@displayOrEdit "unpublish-date" "unpublishDate" editUnpublishDateUrl />
    </div>
  </body>
</html>

<#macro displayOrEdit propName bindName editUrl >
  <#if formName?exists && formName == propName >
    <div class="expandedForm">
      <@spring.bind formName + ".submitURL" />
      <form class="schedule-publishing" action="${spring.status.value?html}" method="post">
        <#assign dateValue = vrtx.propValue(resource, propName, "iso-8601-short") />
        <#assign timeValue = vrtx.propValue(resource, propName, "hours-minutes") />
        <#assign dateTimeValue = "" />
        <#if dateValue?has_content && timeValue?has_content >
          <#assign dateTimeValue = dateValue + " " + timeValue />
        </#if>
        <@spring.bind formName + "." + bindName />
        <#if spring.status.value?exists>
          <#assign dateTimeValue = spring.status.value />
        </#if>
          <h3><@vrtx.msg code="publishing." + propName default="${propName}" />:</h3>
          <@displayValidationErrors spring.status.errorMessages />
          <ul class="property">
          	<li>
          	  <input class="date" type="text" id="${spring.status.expression}" name="${spring.status.expression}" value="${dateTimeValue?html}" />
		  	</li>
		  </ul>
        <div id="submitButtons" class="submitButtons">
          <input type="submit" id="${bindName}UpdateAction" name="${bindName}UpdateAction" value="${vrtx.getMsg("editor.save")}" onclick="saveDateAndTimeFields();"/>
          <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg("editor.cancel")}">
        </div>
      </form>
    </div>
  <#else>

      <h3><@vrtx.msg code="publishing." + propName default="Published date" /></h3>
      <div>
      <#assign editableDate = vrtx.propValue(resource, propName) />
      <#if editableDate?has_content>
        ${editableDate}
      <#else>
        <@vrtx.msg code="publishing.date.not-set" default="Not set" />
      </#if>
      (&nbsp;<a href="${editUrl?html}"><@vrtx.msg code="publishing.edit" default="edit" /></a>&nbsp;)
	  </div>
  </#if>
</#macro>

<#macro displayValidationErrors errorMessages >
  <#if errorMessages?size &gt; 0>
    <div class="errorContainer">
      <ul class="errors">
        <#list spring.status.errorMessages as error>
          <li>${error}</li>
        </#list>
      </ul>
    </div>
  </#if>
</#macro>
