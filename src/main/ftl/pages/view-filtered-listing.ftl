<#ftl strip_whitespace=true>
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>

  </head>
  <body id="vrtx-${resource.resourceType}">

    <#if filters?exists>
      <div id="filters">
        <#list filters?keys as filterKey>
          <#assign filter = filters[filterKey]>
          <div class="filters-section">
            <h2>${filterKey}</h2>
            <#list filter?keys as parameterKey>
              <#assign url = filter[parameterKey].url>
              <#assign marked = filter[parameterKey].marked>
              <#if marked>[x]<#else>[ ]</#if> <a href="${url}" class="filters-parameter ${marked?string}">${parameterKey}</a>
            </#list>
          </div>
        </#list>
      </div>
    </#if>

    <#if (result?exists && result?has_content)>
      <div>
        <#list result as res>
          <#assign title = vrtx.propValue(res, 'title') />
          <#assign uri = vrtx.getUri(res) />
          <p>
            <a href="${uri}">${title}</a>
          </p>
        </#list>
      </div>
    </#if>

    <#if discontinuedUrl?exists>
      <div>
        <a href="${discontinuedUrl}">Discontinued</a>
      </div>
    <#elseif currentUrl?exists>
      <div>
        <a href="${currentUrl}">Current</a>
      </div>
    </#if>

  </body>
</html>