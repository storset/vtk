<#ftl strip_whitespace=true>
<#-- JSON elements/boxes interaction in new documenttypes (add, remove and move etc.) -->

<#macro script>
  <#assign locale = springMacroRequestContext.getLocale() />
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/jquery.scrollTo.min.js"></script>
  <script type="text/javascript"><!--
  
    var addBtn = '${vrtx.getMsg("editor.add")}',
        moveDownBtn = '${vrtx.getMsg("editor.move-down")}',
        moveUpBtn = '${vrtx.getMsg("editor.move-up")}',
        removeBtn = '${vrtx.getMsg("editor.remove")}',
        browseImagesBtn = '<@vrtx.msg code="editor.browseImages" />',
        browseImagesPreview = '<@vrtx.msg code="editor.image.preview-title" />',
        browseImagesNoPreview = '<@vrtx.msg code="editor.image.no-preview-text" />';
        requestLang = '<@vrtx.requestLanguage />',
        parentURI = '${resourceContext.parentURI?js_string}',
        ckBaseURI = '${fckeditorBase.url?html}',
        ckBaseDocURI = '${fckeditorBase.documentURL?html}',
        ckBaseBrowsePath = '${fckBrowse.url.pathRepresentation}';
   
    var TEMPLATES = [];
    var LIST_OF_JSON_ELEMENTS = [];
    var ACCORDION_MOVE_TO_AFTER_CHANGE = null;
    var JSON_ELEMENTS_INITIALIZED = $.Deferred();
    
    $(document).ready(function() {
      // Retrieve HTML templates
      var templatesRetrieved = $.Deferred();
      TEMPLATES = vrtxAdmin.retrieveHTMLTemplates("templates",
                                                  ["string", "html", "radio", "dropdown", "date", "browse", "browse-images", "add-remove-move"],
                                                   templatesRetrieved);
      // Build JSON elements
      var jsonElementsBuilt = $.Deferred();
      <#assign i = 0 />
      <#list form.elements as elementBox>
        <#assign j = 0 />
        <#list elementBox.formElements as elem>
          <#if elem.description.type == "json" && elem.description.isMultiple()>
            LIST_OF_JSON_ELEMENTS[${i}] = {};
            LIST_OF_JSON_ELEMENTS[${i}].name = "${elem.name}";
            LIST_OF_JSON_ELEMENTS[${i}].type = "${elem.description.type}";
            LIST_OF_JSON_ELEMENTS[${i}].a = [];
            
            <#list elem.description.attributes as jsonAttr>
              LIST_OF_JSON_ELEMENTS[${i}].a[${j}] = {};
              LIST_OF_JSON_ELEMENTS[${i}].a[${j}].name = "${jsonAttr.name}";
              LIST_OF_JSON_ELEMENTS[${i}].a[${j}].type = "${jsonAttr.type}";
              <#if jsonAttr.edithints?exists>
                <#if jsonAttr.edithints['dropdown']?exists>
                  LIST_OF_JSON_ELEMENTS[${i}].a[${j}].dropdown = true;
                </#if>
              </#if>
              <#if jsonAttr.getValuemap(locale)?exists >
                <#assign valuemap = jsonAttr.getValuemap(locale) />
                <#assign k = 0 />
                var valuemap = [];
                <#list valuemap?keys as key>
                  <#assign optionKey = key />
                  <#if optionKey = '""' >
                    <#assign optionKey = "''" />
                  </#if>
                  valuemap[${k}] = "${optionKey}$${valuemap[key]}";
                  <#assign k = k + 1 />
                </#list>
                LIST_OF_JSON_ELEMENTS[${i}].a[${j}].valuemap = valuemap;
              </#if>
              LIST_OF_JSON_ELEMENTS[${i}].a[${j}].title = "${form.resource.getLocalizedMsg(jsonAttr.name, locale, null)}";
              <#assign j = j + 1 />
            </#list>
            <#assign i = i + 1 />
          </#if>
        </#list>
      </#list>

      jsonElementsBuilt.resolve();
      
      initJsonMovableElements(templatesRetrieved, jsonElementsBuilt);
      
    });
  // -->
  </script>
</#macro>
