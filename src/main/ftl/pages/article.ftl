<#ftl strip_whitespace=true>

<#--
  - File: article.ftl
  - 
  - Description: Article view
  - 
  - Required model data:
  -   resource
  -
  -->

<#assign title = "Missing title" />
<#assign titleProp = resource.getPropertyByPrefix("", "userTitle")?if_exists />
<#if titleProp?exists>
  <#assign title = titleProp.getFormattedValue() />
</#if>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head><title>${title}</title>

    <style type="text/css">

      body {
        font-family: arial,helvetica,"helvetica neue",sans-serif;
        font-size: 75%;
        color: #636363;
      }
      
      p {line-height: 1.4em; margin-bottom: 0.5em;}
      h1 {
        color: #2b450f;
	font-size: 26px;
	font-weight: normal;
      }
      h2 {
        font-size: 1em;
        font-weight: bold;
      }

      .introduction {font-weight: bold;line-height: 1.4em;}
      .introduction img {float:right; margin: 0 0 .5em .5em;}
      .byline {
	border-top: 1px dotted rgb(160,175,131);
	font-size: 11px;
	line-height: 1.2em;
      }
      .bodyText p {margin-bottom: 18px;
      }
    </style>
</head>
  <body>
    <h1>${title}</h1>

    <#assign introductionProp = resource.getPropertyByPrefix("", "introduction")?if_exists />
    <#if introductionProp?exists>
      <div class="introduction">
        ${introductionProp.getFormattedValue()}
      </div>
    </#if>

    <#assign authorsProp = resource.getPropertyByPrefix("", "authors")?if_exists />
    <#assign publishedProp = resource.getPropertyByPrefix("", "published-date")?if_exists />
<div class="byline">
    <#if authorsProp?exists && publishedProp?exists>
      Av ${authorsProp.getFormattedValue()} <#--<br />${publishedProp.getFormattedValue()?if_exists}-->
    <#elseif authorsProp?exists>
      Av ${authorsProp.getFormattedValue()}
    <#elseif publishedProp?exists>
      ${publishedProp.getFormattedValue()?if_exists}
    </#if>
</div>
        <#assign introductionImageProp = resource.getPropertyByPrefix("", "picture")?if_exists />
        <#if introductionImageProp?exists>
          <img class="introduction" src="${introductionImageProp.getFormattedValue()}" alt="ingressbilde" />
        </#if>
<div class="bodyText">
    ${resourceString}
</div>
  </body>
</html>


