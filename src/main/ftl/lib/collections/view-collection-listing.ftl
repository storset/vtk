<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if (resources?size > 0)>
     <script type="text/javascript" src="/vrtx/__vrtx/static-resources/jquery/include-jquery.js"></script>
     <script type="text/javascript"><!--
       $(function() {
         var agent = navigator.userAgent.toLowerCase();         
         var isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
         if ($.browser.msie && $.browser.version >= 7 && isWin) {  
           $(".vrtx-resource-open-webdav").click(function(e) {
             var openOffice = new ActiveXObject("Sharepoint.OpenDocuments.1").EditDocument(this.href);
             e.stopPropagation();
             e.preventDefault();
           });
           $(".vrtx-resource").hover(function (e) { 
               $(this).find(".vrtx-resource-open-webdav").css("left", ($(this).find(".vrtx-title a").width() + 63) + "px").show(0);
             }, function (e) {
               $(this).find(".vrtx-resource-open-webdav").hide(0).css("left", "0px");
             }
           );
         }
       });
     // -->
     </script>
    <div id="${collectionListing.name}" class="vrtx-resources ${collectionListing.name}">
    <#if collectionListing.title?exists && collectionListing.offset == 0>
      <h2>${collectionListing.title?html}</h2>
    </#if>
    
    <#list resources as r>
      <#assign uri = vrtx.getUri(r) />
      
      <#if !hideIcon?exists>
        <div class="vrtx-resource vrtx-resource-icon">
      <#else>
        <div class="vrtx-resource">
      </#if>
        <#if !hideIcon?exists>
		  <a class="vrtx-icon <@vrtx.iconResolver r.resourceType r.contentType />" href="${uri?html}"></a>
		</#if> 
      
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(r, "title", "", "") />
		  <#if !title?has_content && solrUrl?exists && solrUrl?has_content>
		    <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		  </#if>
          <a class="vrtx-title" href="${uri?html}">${title?html}</a>
          <#if (r.resourceType == "doc" || r.resourceType == "xls" || r.resourceType == "ppt")>
            <#if msofficeDocsWritable[r_index] = "true">
              <a class="vrtx-resource-open-webdav" href="${vrtx.linkConstructor(uri, 'webdavService')}"><@vrtx.msg code="report.collection-structure.edit" /></a>
            </#if>
          </#if>
		</div>

        <#list collectionListing.displayPropDefs as displayPropDef>
          <#if displayPropDef.name = 'introduction'>
            <#assign val = vrtx.getIntroduction(r) />
          <#elseif displayPropDef.type = 'IMAGE_REF'>
            <#assign val><img src="${vrtx.propValue(r, displayPropDef.name, "")}" /></#assign>
          <#elseif displayPropDef.name = 'lastModified'>
            <#assign val>
              <@vrtx.msg code="viewCollectionListing.lastModified"
                         args=[vrtx.propValue(r, displayPropDef.name, "long")] />
            </#assign>
            <#assign val = val + " " + vrtx.propValue(r, 'modifiedBy', 'link') />
          <#else>
            <#assign val = vrtx.propValue(r, displayPropDef.name, "long") /> <#-- Default to 'long' format -->
          </#if>

          <#if val?has_content>
            <div class="${displayPropDef.name}">
              ${val}
            </div>
          </#if>
        </#list>
      </div>
    </#list>
   </div>
  </#if>
  
</#macro>