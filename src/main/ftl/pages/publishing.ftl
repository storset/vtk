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
    
    <#assign language = vrtx.getMsg("eventListing.calendar.lang", "en") />
    
    <#if language = "no">
      <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-no.js"></script>
    </#if>
    <#if language = "nn">
      <script type="text/javascript" src="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/js/jquery.ui.datepicker-nn.js"></script>
    </#if>
    <script type="text/javascript" src="${jsBaseURL?html}/datepicker/datepicker-admin.js"></script>

    <script type="text/javascript">
    <!--
      $(document).ready(function() {
          initDatePicker("${language}");
       });
    //-->
    </script>
    
    <link type="text/css" href="${webResources?html}/jquery/plugins/ui/jquery-ui-1.8.8.custom/css/smoothness/jquery-ui-1.8.8.custom.css" rel="stylesheet" />
  </head>
  <#assign resource = resourceContext.currentResource />
  <#assign header = vrtx.getMsg("publishing.header", "Publishing status on document") />
  <body id="vrtx-publishing">
    <div class="resourceInfo publishing">
      <h2>
        ${header}
      </h2>
      <div id="publishing-status">
        <#assign isPublished = resource.isPublished() />
        <#assign publishedStatusMsgKey = "publishing.status." + isPublished?string />
        <h3><@vrtx.msg code="publishing.status" default="Status" /></h3>
        <div>
        <@vrtx.msg code=publishedStatusMsgKey default="" />
        <#if isPublished>
          <#assign titleMsg = vrtx.getMsg("confirm-publish.title.unpublish") />
          &nbsp;<a class="vrtx-button-small thickbox" href="${unPublishUrl?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" title="${titleMsg}"><span><@vrtx.msg code="publish.action.unpublish" default="Unpublish" /></span></a>
        <#else>
          <#assign titleMsg = vrtx.getMsg("confirm-publish.title.publish") />
          &nbsp;<a class="vrtx-button-small thickbox" href="${publishUrl?html}&amp;showAsHtml=true&amp;height=80&amp;width=230" title="${titleMsg}"><span><@vrtx.msg code="publish.action.publish" default="Publish" /></span></a>
        </#if>
        </div>
      </div>
        
      <@displayOrEdit "publish-date" "publishDate" editPublishDateUrl />
      <@displayOrEdit "unpublish-date" "unpublishDate" editUnpublishDateUrl />
    </div>
  </body>
</html>

<#macro displayOrEdit propName bindName editUrl >
  <#if formName?exists && formName == propName >
    <div id="publishing-${propName}" class="expandedForm">
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
          	  <div class="vrtx-textfield">
          	    <input class="date" type="text" id="${spring.status.expression}" name="${spring.status.expression}" value="${dateTimeValue?html}" />
          	  </div>
		  	</li>
		  </ul>
        <div id="submitButtons" class="submitButtons">
          <div class="vrtx-focus-button">
            <input type="submit" id="${bindName}UpdateAction" name="${bindName}UpdateAction" value="${vrtx.getMsg("editor.save")}" onclick="saveDateAndTimeFields();"/>
          </div>
          <div class="vrtx-button">
            <input type="submit" id="cancelAction" name="cancelAction" value="${vrtx.getMsg("editor.cancel")}">
          </div>
        </div>
      </form>
    </div>
  <#else>
    <div id="publishing-${propName}">
      <h3><@vrtx.msg code="publishing." + propName default="Published date" /></h3>
      <div>
      <#assign editableDate = vrtx.propValue(resource, propName) />
      <#if editableDate?has_content>
        ${editableDate}
      <#else>
        <@vrtx.msg code="publishing.date.not-set" default="Not set" />
      </#if>
      &nbsp;<a class="vrtx-button-small" href="${editUrl?html}"><span><@vrtx.msg code="publishing.edit" default="edit" /></span></a>
	  </div>
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
