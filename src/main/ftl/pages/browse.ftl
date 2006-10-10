<#ftl strip_whitespace=true>

<#--
  - File: browse.ftl
  - 
  - Description: A HTML page that displays a listing of resources
  - with the purpose of locating a resource through navigation and
  - creating a link to that resource.
  - 
  - Required model data:
  -   resourceContext
  -
  - Optional model data:
  -
  -->

<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collectionlisting.ftl" as col />

<#if !resourceContext?exists>
  <#stop "Unable to render model: required model data
  'resourceContext' missing">
</#if>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Browse</title>
</head>
<body>
	
  <#if resourceContext.currentResource.collection>
    <@col.listCollection />
  </#if>

</body>
</html>
