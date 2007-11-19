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

<#function propValue propName>
<#local prop = resource.getPropertyByPrefix("", propName)?default("") />
<#if prop != "">
  <#return prop.getFormattedValue() />
</#if>
<#return "" />
</#function>

<#assign title = propValue("userTitle") />
<#if title == "">
  <#assign title = "Missing title" />
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

    <#assign introduction = propValue("introduction") />
    <#if introduction != "">
      <div class="introduction">
        ${introduction}
      </div>
    </#if>

    <#assign authors = propValue("authors") />
    <#assign published = propValue("published-date") />
<div class="byline">
    <#if authors != "" && published != "">
      Av ${authors} <br />${published}
    <#elseif authors != "">
      Av ${authors}
    <#elseif published != "">
      ${published}
    </#if>
</div>
        <#assign introductionImage = propValue("introduction-image") />
        <#if introductionImage != "">
          <img class="introduction" src="${introductionImage}" alt="ingressbilde" />
        </#if>
        <div class="bodyText">
    ${resourceString}
</div>
  </body>
</html>


