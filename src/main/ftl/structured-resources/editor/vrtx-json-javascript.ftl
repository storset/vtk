<#ftl strip_whitespace=true>
<#-- JSON elements interaction in new documenttypes (add, remove and move) 

     TODO: * Move all JS to JS-file to get syntax highlighting
           * Still a little too much traversal and i think a little slow solution for replacing id-numbers on move

-->

<#macro script>
  <#assign locale = springMacroRequestContext.getLocale() />
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/jquery.scrollTo.min.js"></script>
  <script type="text/javascript"><!--
   
    var TEMPLATES = [];
    var LIST_OF_JSON_ELEMENTS = [];
    var ACCORDION_MOVE_TO_AFTER_CHANGE = null;
    var JSON_ELEMENTS_INITIALIZED = $.Deferred(); 
    
    $(document).ready(function() {

      // Retrieve HTML templates
      var templatesRetrieved = $.Deferred();
      TEMPLATES = vrtxAdmin.retrieveHTMLTemplates("templates",
                                                  ["string", "html", "radio", "dropdown", "date", "browse", "add-remove-move"],
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

      $.when(templatesRetrieved, jsonElementsBuilt).done(function() {
        for (var i = 0, len = LIST_OF_JSON_ELEMENTS.length; i < len; i++) {
          var json = { clazz: "add", buttonText: '${vrtx.getMsg("editor.add")}' }
          $("#" + LIST_OF_JSON_ELEMENTS[i].name)
            .append($.mustache(TEMPLATES["add-remove-move"], json))
            .find(".vrtx-add-button").data({'number': i});
        }
        
        // TODO: avoid this being hardcoded here
        var syllbausItems = $("#editor.vrtx-syllabus #items");
        wrapJSONItemsLeftRight(syllbausItems.find(".vrtx-json-element"), ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
        syllbausItems.find(".author input, .title input").addClass("header-populators");
        syllbausItems.find(".vrtx-html textarea").addClass("header-fallback-populator");
        
        var sharedTextItems = $("#editor.vrtx-shared-text #shared-text-box");
        sharedTextItems.find(".title input").addClass("header-populators");
        // ^ TODO: avoid this being hardcoded here
      
        // Because accordion needs one content wrapper
        for(var grouped = $(".vrtx-json-accordion .vrtx-json-element"), i = grouped.length; i--;) { 
          var group = $(grouped[i]);
          group.find("> *").wrapAll("<div />");
          updateAccordionHeader(group);
        }
      
        $(".vrtx-json-accordion .fieldset").accordion({ 
                                              header: "> div > .header",
                                              autoHeight: false,
                                              collapsible: true,
                                              active: false,
                                              change: function(e, ui) {
                                                updateAccordionHeader(ui.oldHeader);
                                                if(ACCORDION_MOVE_TO_AFTER_CHANGE) {
                                                  scrollToElm(ACCORDION_MOVE_TO_AFTER_CHANGE);
                                                }
                                              }  
                                            });
        JSON_ELEMENTS_INITIALIZED.resolve();
      });

      var appContent = $("#app-content");
      appContent.on("click", ".vrtx-json .vrtx-add-button", function(e) {
        var accordionWrapper = $(this).closest(".vrtx-json-accordion");
        var hasAccordion = accordionWrapper.length;
           
        var btn = $(this);
        var jsonParent = btn.closest(".vrtx-json");
        var counter = jsonParent.find(".vrtx-json-element").length;
        var j = LIST_OF_JSON_ELEMENTS[parseInt(btn.data('number'))];
        var htmlTemplate = "";
        var arrayOfIds = [];

        // Add correct HTML for vrtx-type
        var types = j.a;

        for (var i in types) {
          var inputFieldName = j.name + "." + types[i].name + "." + counter;
          arrayOfIds[i] = new String(j.name + "." + types[i].name + ".").replace(/\./g, "\\.");
          switch (types[i].type) {
            case "string":
              if (types[i].dropdown && types[i].valuemap) {
                htmlTemplate += addDropdown(types[i], inputFieldName);
              } else {
                htmlTemplate += addStringField(types[i], inputFieldName);
              }
              break;
            case "html":
              htmlTemplate += addHtmlField(types[i], inputFieldName);
              break;
            case "simple_html":
              htmlTemplate += addHtmlField(types[i], inputFieldName);
              break;
            case "boolean":
              htmlTemplate += addBooleanField(types[i], inputFieldName);
              break;
            case "image_ref":
              htmlTemplate += addImageRef(types[i], inputFieldName);
              break;
            case "resource_ref":
              htmlTemplate += addResourceRef(types[i], inputFieldName);
              break;
            case "datetime":
              htmlTemplate += addDateField(j.a[i], inputFieldName);
              break;
            case "media":
              htmlTemplate += addMediaRef(types[i], inputFieldName);
              break;
            default:
              htmlTemplate += "";
              break;
          }
        }
      
        // Move up, move down, remove
        var isImmovable = jsonParent && jsonParent.hasClass("vrtx-multiple-immovable");

        if(!isImmovable) {
          var moveDownButton = $.mustache(TEMPLATES["add-remove-move"], { clazz: 'move-down', buttonText: '&darr; ${vrtx.getMsg("editor.move-down")}' });
          var moveUpButton = $.mustache(TEMPLATES["add-remove-move"],   { clazz: 'move-up',   buttonText: '&uarr; ${vrtx.getMsg("editor.move-up")}'   });
        }
        var removeButton = $.mustache(TEMPLATES["add-remove-move"],   { clazz: 'remove',    buttonText: '${vrtx.getMsg("editor.remove")}'           });
      
        var id = "<input type=\"hidden\" class=\"id\" value=\"" + counter + "\" \/>";
        var newElementId = "vrtx-json-element-" + j.name + "-" + counter;
        
        var newElementHtml = htmlTemplate;
        newElementHtml += id;
        newElementHtml += removeButton;
    
        if (!isImmovable && counter > 0) {
          newElementHtml += moveUpButton;
        }
      
        $("#" + j.name + " .vrtx-add-button").before("<div class='vrtx-json-element' id='" + newElementId + "'>" + newElementHtml + "<\/div>");
      
        var newElement = $("#" + newElementId);
        var prev = newElement.prev(".vrtx-json-element");
        newElement.addClass("last");
        prev.removeClass("last");
      
        if(!isImmovable && counter > 0) {
          newElement.find(".vrtx-move-up-button").click(function (e) {
            swapContent(counter, arrayOfIds, -1, j.name);
            e.stopPropagation();
            e.preventDefault();
          });

          if (prev.length) {
            if(hasAccordion) {
              prev.find("> div.ui-accordion-content").append(moveDownButton);
            } else {
              prev.append(moveDownButton);
            }
            prev.find(".vrtx-move-down-button").click(function (e) {
              swapContent(counter-1, arrayOfIds, 1, j.name);
              e.stopPropagation();
              e.preventDefault();
            });
          }
        }
        
        if(hasAccordion ) {
          var accordionContent = accordionWrapper.find(".fieldset");

          var group = accordionContent.find(".vrtx-json-element:last");
          group.find("> *").wrapAll("<div />");
          group.prepend('<div class="header">' + (vrtxAdmin.lang !== "en" ? "Inget innhold" : "No content") + '</div>');
          
          // TODO: avoid this being hardcoded here
          var lastSyllabusItem = $("#editor.vrtx-syllabus #items .vrtx-json-element:last");
          wrapJSONItemsLeftRight(lastSyllabusItem, ".author, .title, .year, .publisher, .isbn, .comment", ".linktext, .link, .bibsys, .fulltext, .articles");
          lastSyllabusItem.find(".author input, .title input").addClass("header-populators");
          lastSyllabusItem.find(".vrtx-html textarea").addClass("header-fallback-populator");
          
          var lastSharedTextItem = $("#editor.vrtx-shared-text #shared-text-box .vrtx-json-element:last");
          lastSharedTextItem.find(".title input").addClass("header-populators");
          // ^ TODO: avoid this being hardcoded here
          
          accordionRefresh(accordionContent, false);
        }

        // CK and date inputfields

        for (i in types) {
          var inputFieldName = j.name + "." + types[i].name + "." + counter;
          if (types[i].type == "simple_html") {
            newEditor(inputFieldName, false, false, '${resourceContext.parentURI?js_string}', '${fckeditorBase.url?html}', 
                                                    '${fckeditorBase.documentURL?html}', '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, "true");
          } else if (types[i].type == "html") {
            newEditor(inputFieldName, true, false, '${resourceContext.parentURI?js_string}', '${fckeditorBase.url?html}', 
                                                   '${fckeditorBase.documentURL?html}', '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', cssFileList, "false");
          } else if (types[i].type == "datetime") {
            displayDateAsMultipleInputFields(inputFieldName);
          }
        }

        e.stopPropagation();
        e.preventDefault();
      });

      appContent.on("click", ".vrtx-json .vrtx-remove-button", function(e) {
        var removeElement = $(this).closest(".vrtx-json-element");
        var accordionWrapper = removeElement.closest(".vrtx-json-accordion");
        var hasAccordion = accordionWrapper.length;
        
        var removeElementParent = removeElement.parent();
        var textAreas = removeElement.find("textarea");
        var i = textAreas.length;
        while(i--) {
          var textAreaName = textAreas[i].name;
          if (isCkEditor(textAreaName)) {
            var ckInstance = getCkInstance(textAreaName);
            ckInstance.destroy();
            delete ckInstance;
          }
        }
        removeElement.remove();
        removeElementParent.find(".vrtx-json-element:first .vrtx-move-up-button").remove();
        removeElementParent.find(".vrtx-json-element:last .vrtx-move-down-button").remove();
        if(hasAccordion) {
          var accordionContent = accordionWrapper.find(".fieldset");
          accordionRefresh(accordionContent, false);
        }
        e.stopPropagation();
        e.preventDefault();
      });
    });

    function accordionRefresh(elem, active) {
      elem.accordion('destroy').accordion({ 
                                  header: "> div > .header",
                                  autoHeight: false,
                                  collapsible: true,
                                  active: active,
                                  change: function(e, ui) {
                                    updateAccordionHeader(ui.oldHeader);
                                    if(ACCORDION_MOVE_TO_AFTER_CHANGE) {
                                      scrollToElm(ACCORDION_MOVE_TO_AFTER_CHANGE);
                                    }
                                  }  
                                });
    }
    
    function wrapJSONItemsLeftRight(items, leftItems, rightItems) {
      if(items.length == 1) {
        items.find(leftItems).wrapAll("<div class='left' />");
        items.find(rightItems).wrapAll("<div class='right' />");
      } else if(items.length > 1) {
         var i = items.length;
         while(i--) {
           $(items[i]).find(leftItems).wrapAll("<div class='left' />");
           $(items[i]).find(rightItems).wrapAll("<div class='right' />");
         }
      }
    }
    
    function updateAccordionHeader(elem) {
      var jsonElm = elem.closest(".vrtx-json-element");
      if(jsonElm.length) { // Prime header populators
        var str = "";
        var fields = jsonElm.find(".header-populators");
        for(var i = 0, len = fields.length; i < len; i++) {
          var val = $(fields[i]).val(); 
          if(!val.length) continue;
          str += (str.length) ? ", " + val : val;
        }
        if(!str.length) { // Fallback header populator
          var field = jsonElm.find(".header-fallback-populator");
          if(field.length) {
            var fieldId = field.attr("id");
            if(isCkEditor(fieldId)) { // Check if CK
              str = getCkValue(fieldId); // Get CK content
            } else {
              str = field.val();
            }
            if(field.is("textarea")) { // Remove markup and tabs
              str = $.trim(str.replace(/(<([^>]+)>|[\t\r]+)/ig, ""));
            }
            if(typeof str !== "undefined") {
              if(str.length > 30) {
                str = str.substring(0, 30) + "...";
              } else if(!str.length) {
                str = (vrtxAdmin.lang !== "en") ? "Inget innhold" : "No content";
              }
            }
          } else {
            str = (vrtxAdmin.lang !== "en") ? "Inget innhold" : "No content";
          }
        }
        var header = jsonElm.find("> .header");
        if(!header.length) {
          jsonElm.prepend('<div class="header">' + str + '</div>');
        } else {
          header.html('<span class="ui-icon ui-icon-triangle-1-e"></span>' + str);
        }
      }
    }

    function addStringField(elem, inputFieldName) {
      var json = { classes: "vrtx-string" + " " + elem.name,
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName }
      return $.mustache(TEMPLATES["string"], json); 
    }
    
    function addHtmlField(elem, inputFieldName) {
      var baseclass = "vrtx-html";
      if (elem.type == "simple_html") {
        baseclass = "vrtx-simple-html";
      }
      var json = { classes: baseclass + " " + elem.name,
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName }
      return $.mustache(TEMPLATES["html"], json); 
    }
    
    function addBooleanField(elem, inputFieldName) {
      var json = { elemTitle: elem.title,
                   inputFieldName: inputFieldName }
      return $.mustache(TEMPLATES["radio"], json); 
    }

    function addDropdown(elem, inputFieldName) {
      var htmlOpts = [];
      for (i in elem.valuemap) {
        var keyValuePair = elem.valuemap[i];
        var keyValuePairSplit = keyValuePair.split("$");
        htmlOpts.push({key: keyValuePairSplit[0], value: keyValuePairSplit[1]});
      }
      var json = { classes: "vrtx-string" + " " + elem.name,
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName,
                   options: htmlOpts }
      return $.mustache(TEMPLATES["dropdown"], json);  
    }

    function addDateField(elem, inputFieldName) {
      var json = { elemTitle: elem.title,
                   inputFieldName: inputFieldName }
      return $.mustache(TEMPLATES["date"], json); 
    }
    
    function addImageRef(elem, inputFieldName) {
      var json = { clazz: 'vrtx-image-ref',
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName,
                   fckEditorBaseUrl: '${fckeditorBase.url}',
                   parentURI: '${resourceContext.parentURI?js_string}',
                   fckBrowsePath: '${fckBrowse.url.pathRepresentation}',
                   browseButtonText: '<@vrtx.msg code="editor.browseImages" />',
                   type: '',
                   size: 30,
                   onBlur: "previewImage('" + inputFieldName + "');",
                   preview: "<div id='" + inputFieldName + ".preview'></div>" }
      return $.mustache(TEMPLATES["browse"], json); 
    }
    
    function addResourceRef(elem, inputFieldName) {
      var json = { clazz: 'vrtx-resource-ref',
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName,
                   fckEditorBaseUrl: '${fckeditorBase.url}',
                   parentURI: '${resourceContext.parentURI?js_string}',
                   fckBrowsePath: '${fckBrowse.url.pathRepresentation}',
                   browseButtonText: '<@vrtx.msg code="editor.browseImages" />',
                   type: 'File',
                   size: 40,
                   onBlur: "",
                   preview: "" }
      return $.mustache(TEMPLATES["browse"], json); 
    }
    
    function addMediaRef(elem, inputFieldName) {
      var json = { clazz: 'vrtx-media-ref',
                   elemTitle: elem.title,
                   inputFieldName: inputFieldName,
                   fckEditorBaseUrl: '${fckeditorBase.url}',
                   parentURI: '${resourceContext.parentURI?js_string}',
                   fckBrowsePath: '${fckBrowse.url.pathRepresentation}',
                   browseButtonText: '<@vrtx.msg code="editor.browseImages" />',
                   type: 'Media',
                   size: 30,
                   onBlur: "",
                   preview: "" }      
      return $.mustache(TEMPLATES["browse"], json); 
    }
    
    // Move up or move down
    
    function swapContent(counter, arrayOfIds, move, name) {
      var thisId = "#vrtx-json-element-" + name + "-" + counter;
      var thisElm = $(thisId);
      
      var accordionWrapper = thisElm.closest(".vrtx-json-accordion");
      var hasAccordion = accordionWrapper.length;   
      
      if (move > 0) {
        var movedElm = thisElm.next(".vrtx-json-element");
      } else {
        var movedElm = thisElm.prev(".vrtx-json-element");
      }
      var movedId = "#" + movedElm.attr("id");
      var moveToCounter = movedElm.find("input.id").val();
      
      for (var x = 0, len = arrayOfIds.length; x < len; x++) {
        var elementId1 = '#' + arrayOfIds[x] + counter;
        var elementId2 = '#' + arrayOfIds[x] + moveToCounter;
        var element1 = $(elementId1);
        var element2 = $(elementId2);
        
        /* We need to handle special cases like CK fields and date */
        
        var ckInstanceName1 = arrayOfIds[x].replace(/\\/g, '') + counter;
        var ckInstanceName2 = arrayOfIds[x].replace(/\\/g, '') + moveToCounter;

        if (isCkEditor(ckInstanceName1) && isCkEditor(ckInstanceName2)) {
          var val1 = getCkValue(ckInstanceName1);
          var val2 = getCkValue(ckInstanceName2);
          setCkValue(ckInstanceName1, val2);
          setCkValue(ckInstanceName2, val1);
        } else if (element1.hasClass("date") && element2.hasClass("date")) {
          var element1Wrapper = element1.closest(".vrtx-string");
          var date1 = element1Wrapper.find(elementId1 + '-date');
          var hours1 = element1Wrapper.find(elementId1 + '-hours');
          var minutes1 = element1Wrapper.find(elementId1 + '-minutes');
          var element2Wrapper = element2.closest(".vrtx-string");
          var date2 = element2Wrapper.find(elementId2 + '-date');
          var hours2 = element2Wrapper.find(elementId2 + '-hours');
          var minutes2 = element2Wrapper.find(elementId2 + '-minutes');
          var dateVal1 = date1.val();
          var hoursVal1 = hours1.val();
          var minutesVal1 = minutes1.val();
          var dateVal2 = date2.val();
          var hoursVal2 = hours2.val();
          var minutesVal2 = minutes2.val();
          date1.val(dateVal2);
          hours1.val(hoursVal2);
          minutes1.val(minutesVal2);
          date2.val(dateVal1);
          hours2.val(hoursVal1);
          minutes2.val(minutesVal1);
        }    
        
        var val1 = element1.val();
        var val2 = element2.val();
        element1.val(val2);
        element2.val(val1);
        if(hasAccordion) {
          updateAccordionHeader(element1);
          updateAccordionHeader(element2);
        }
        element1.blur();
        element2.blur();
        element1.change();
        element2.change();
      }
      thisElm.focusout();
      movedElm.focusout();
      
      if(hasAccordion) {
        ACCORDION_MOVE_TO_AFTER_CHANGE = movedElm;
        var accordionContent = accordionWrapper.find(".fieldset");
        accordionContent.accordion("option", "active", (movedElm.index() - 1));
        accordionContent.accordion("option", "refresh");
      } else {
        scrollToElm(movedElm);
      }
    }
    
    function scrollToElm(movedElm) {
      var absPos = movedElm.offset();
      var absPosTop = absPos.top;
      var stickyBar = $("#vrtx-editor-title-submit-buttons");
      if(stickyBar.css("position") == "fixed") {
        var stickyBarHeight = stickyBar.height();
        absPosTop -= (stickyBarHeight <= absPosTop) ? stickyBarHeight : 0;
      }
      
      $('body').scrollTo(absPosTop, 250, {
        easing: 'swing',
        queue: true,
        axis: 'y'
      });
      setTimeout(function() {
        ACCORDION_MOVE_TO_AFTER_CHANGE = null;
      }, 270);
    }
  
  // -->
  </script>
</#macro>
