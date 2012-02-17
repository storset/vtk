<#import "/lib/vortikal.ftl" as vrtx />
<#import "/lib/view-utils.ftl" as viewutils />

<#macro displayCollection collectionListing>

  <#local resources=collectionListing.files />
  <#if (resources?size > 0)>
     <script type="text/javascript"><!--
       $(function() {
         var agent = navigator.userAgent.toLowerCase();         
         var isWin = ((agent.indexOf("win") != -1) || (agent.indexOf("16bit") != -1));
         if ($.browser.msie && $.browser.version >= 5 && isWin) {  
           $(".vrtx-resource-open-webdav").show(0).click(function(e) {
             var openOffice = new ActiveXObject("Sharepoint.OpenDocuments.1").EditDocument(this.href);
             e.stopPropagation();
             e.preventDefault();
           });
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
      <#assign webdavUri = webdavUrls[r_index] />

      <div class="vrtx-resource">
        <#if !hideIcon?exists>
		  <a class="vrtx-icon <@vrtx.iconResolver r.resourceType r.contentType />" href="${uri?html}"></a>
		</#if> 
      
		<div class="vrtx-title">
		  <#assign title = vrtx.propValue(r, "title", "", "") />
		  <#if !title?has_content && solrUrl?exists && solrUrl?has_content>
		    <#assign title = vrtx.propValue(r, "solr.name", "", "") />
		  </#if>
          <a class="vrtx-title" href="${uri?html}">${title?html}</a>
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
        <#-- TODO: Only show when authorized to write --> 
        <#if r.resourceType == "doc" || r.resourceType == "xls" || r.resourceType == "ppt">
          &nbsp;
          <a class="vrtx-resource-open-webdav" href="${webdavUri?html}"><@vrtx.msg code="tabs.editService" /></a>
        </#if>
        <span class="vrtx-resource-seperator"></span>
      </div>
    </#list>
   </div>
  </#if>
  
</#macro>