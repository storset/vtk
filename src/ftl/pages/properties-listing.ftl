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
  <p>Lorem ipsum dolere sit amet...</p>


  <h3 class="resourceInfoHeader">
    <@vrtx.msg
       code="resource.metadata.about.basic-information"
       default="Basic information"/>
  </h3>
  <@propList.propertyList basicPropList />


  <h3 class="resourceInfoHeader">
      <@vrtx.msg code="resourceProperties.visualProfile" default="UiOs visuelle profil"/>
  </h3>
  <@propList.propertyList uhtmlPropList />


  <h3 class="resourceInfoHeader">
      <@vrtx.msg code="resourceProperties.information-describing-content" default="Information describing the content"/>
  </h3>
  <@propList.propertyList informationDescribingContentPropList />

  <h3 class="resourceInfoHeader">
      <@vrtx.msg code="resourceProperties.technical-details" default="Technical details"/>
  </h3
  <@propList.propertyList technicalDetailsPropList />




    <!-- @propList.propertyList standardPropList / -->


</body>
</html>
