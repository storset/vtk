<#ftl strip_whitespace=true>

<#--
  - File: properties-listing.ftl
  - 
  - Description: A HTML page that displays resource properties
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/propertyList.ftl" as propList />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>Properties</title>
</head>
<body>

<#assign defaultHeader = vrtx.getMsg("resource.metadata.about", "About this resource") />
<div class="resourceInfoHeader" style="padding-top:0;padding-bottom:1.5em;">
  <h2 style="padding-top: 0px;float:left;">
    <@vrtx.msg
       code="resource.metadata.about.${resourceContext.currentResource.resourceType}"
       default="${defaultHeader}"/>
  </h2>
</div>

  <@propList.propertyList standardPropList />

      <h2 class="resourceInfoHeader" style="padding-top:15px;">
        <@vrtx.msg code="resourceProperties.visualProfile" default="UiOs visuelle profil"/>
      </h2>

  <@propList.propertyList uhtmlPropList />

</body>
</html>
