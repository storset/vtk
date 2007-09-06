<#ftl strip_whitespace=true>

<#--
  - File: view-collectionlisting.ftl
  - 
  - Description: A HTML page that displays a collection listing.
  - 
  - Required model data:
  -   resourceContext
  -   collectionListing
  -  
  - Optional model data:
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/collectionlisting.ftl" as col />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title><@vrtx.msg code="title.collectionListing" args=[resourceContext.currentResource.title] default="Collection listing" /></title>
</head>
<body>
  <@col.listCollection withForm=false />
</body>
</html>
