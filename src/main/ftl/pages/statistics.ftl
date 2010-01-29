
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
  </head>
  <body>
  <#if form?exists>
    <@displayForm />
  <#else>
    <@displayStatistics />
  </#if>
  </body>
</html>

<#macro displayForm>
  <@spring.bind form + ".submitURL" />
  <form class="vrtx-statistics-search" action="${spring.status.value?html}" method="post">

    <@spring.bind form + ".allResources" />
    <#if spring.status.errorMessages?size &gt; 0>
      <div class="errorContainer">
        <ul class="errors">
          <#list spring.status.errorMessages as error> 
            <li>${error}</li> 
          </#list>
        </ul>
      </div>
    </#if>
    <input name="allResources" id="allResources" type="radio" value="true" <#if spring.status.value?string == "true" > checked="checked" </#if> />
      <@vrtx.msg code="statistics.all.resources" default="All document types" /><br/>
    <input name="allResources" id="allResources" type="radio" value="false" <#if spring.status.value?string == "false" > checked="checked" </#if> />
      <@vrtx.msg code="statistics.selected.resources" default="Following document types" />:<br/>

    <div id="submitButtons" class="submitButtons">
      <input type="submit" id="statisticsGetAction" name="statisticsGetAction" value="${vrtx.getMsg("statistics.get")}"/>
    </div>

  </form>
</#macro>

<#macro displayStatistics>
  <div class="vrtx-statistics-options">
    <a href="${returnURL}"><@vrtx.msg code="statistics.return" default="Back" /></a>
  </div>
  <div class="vrtx-statistics-table">
    <table>
      <thead>
        <tr>
          <th>${vrtx.getMsg("collectionListing.name")}</th>
        </tr>
      </thead>
      <tbody>
      <#list resources as resource>
        <tr>
          <td class="vrtx-statistics-resource"><a href="${resource.URI}">${resource.URI}</a></td>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>
</#macro>
