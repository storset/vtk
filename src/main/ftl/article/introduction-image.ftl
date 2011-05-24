<#ftl strip_whitespace=true>

<#--
  - File: introduction-image.ftl
  - 
  - Description: Article introduction image
  - 
  - Required model data:
  -   resource
  -
  -->
<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#assign resource = resourceContext.currentResource />
<#assign imageRes = vrtx.propResource(resource, "picture") />
<#assign introductionImage = vrtx.propValue(resource, "picture") />
<#assign caption = vrtx.propValue(resource, "caption") />
  
<#-- Flattened caption for alt-tag in image -->
<#assign captionFlattened>
    <@vrtx.flattenHtml value=caption escape=true />
</#assign>
  
<#if introductionImage != "">
  <#if imageRes == "">
    <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
  <#else>
    <#assign pixelWidth = imageRes.getValueByName("pixelWidth")?default("") />
    <#assign photographer = imageRes.getValueByName("photographer")?default("") />
      
    <#assign style="" />
    <#if pixelWidth != "">
       <#assign style = "width:" + pixelWidth+ "px;" />
    </#if>
      
    <#if caption != ""><#-- Caption is set -->
      <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
               <img src="${introductionImage}" alt="${captionFlattened}" />
          <div class="vrtx-imagetext">
               <div class="vrtx-imagedescription">${caption}</div>
               <span class="vrtx-photo">
                  <#if photographer != ""><#-- Image authors is set -->
                    <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${photographer}
                  </#if>
               </span>
         </div> 
     </div>
    <#else>
       <#if photographer != ""><#-- No caption but image author set -->
          <div class="vrtx-introduction-image" <#if style?has_content>style="${style}"</#if>>
          <img src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />   
            <div class="vrtx-imagetext">
              <span class="vrtx-photo">
                <span class="vrtx-photo-prefix"><@vrtx.msg code="article.photoprefix" />: </span>${photographer}
              </span>
            </div>     
          </div>
       <#else><#-- No caption or image author set -->
          <img class="vrtx-introduction-image" src="${introductionImage}" alt="${vrtx.getMsg("article.introductionImageAlt")}" />
       </#if>
        </#if>
  </#if>
</#if>
