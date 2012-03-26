<#ftl strip_whitespace=true>
<#-- JSON elements interaction in new documenttypes (add, remove and move) -->

<#macro script>
  <#assign locale = springMacroRequestContext.getLocale() />
  <script type="text/javascript" src="${webResources?html}/jquery/plugins/jquery.scrollTo-1.4.2-min.js"></script>

  <script type="text/javascript"> <!--
   
    LIST_OF_JSON_ELEMENTS = [];

    $(document).ready(function() {

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

      for (var i = 0, len = LIST_OF_JSON_ELEMENTS.length; i < len; i++) {
        $("#" + LIST_OF_JSON_ELEMENTS[i].name).append("<div class=\"vrtx-button vrtx-add-button\" onClick=\"addNewJsonElement(LIST_OF_JSON_ELEMENTS["
                                                    + i + "],this)\"><input type=\"button\" value=\"${vrtx.getMsg("editor.add")}\" /></div>");
      }

    });

    function addNewJsonElement(j, button) {
    
      var counter = parseInt($(button).prev(".vrtx-json-element").find("input.id").val()) + 1;
      if (isNaN(counter)) {
        counter = 0;
      }

      var htmlTemplate = "";
      var arrayOfIds = [];

      // Add correct HTML for vrtx-type

      for (i in j.a) {
        var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
        arrayOfIds[i] = new String(j.name + "." + j.a[i].name + ".").replace(/\./g, "\\.");
        switch (j.a[i].type) {
          case "string":
            if (j.a[i].dropdown && j.a[i].valuemap) {
              htmlTemplate += addDropdown(j.a[i], inputFieldName);
              break;
            } else {
              htmlTemplate += addStringField(j.a[i], inputFieldName);
              break
            }
          case "html":
            htmlTemplate += addHtmlField(j.a[i], inputFieldName);
            break
          case "simple_html":
            htmlTemplate += addHtmlField(j.a[i], inputFieldName);
            break
          case "boolean":
            htmlTemplate += addBooleanField(j.a[i], inputFieldName);
            break
          case "image_ref":
            htmlTemplate += addImageRef(j.a[i], inputFieldName);
            break
          case "resource_ref":
            htmlTemplate += addResourceRef(j.a[i], inputFieldName);
            break
          case "datetime":
            htmlTemplate += addDateField(j.a[i], inputFieldName);
            break
          case "media":
            htmlTemplate += addMediaRef(j.a[i], inputFieldName);
            break
          default:
            htmlTemplate += "";
            break
        }
      }
      
      // Move up, move down, delete

      var moveDownButton = "<div class=\"vrtx-button vrtx-move-down-button\"><input type=\"button\" value=\"&darr; ${vrtx.getMsg("editor.move-down")}\" \/><\/div>";
      var moveUpButton = "<div class=\"vrtx-button vrtx-move-up-button\"><input type=\"button\" value=\"&uarr; ${vrtx.getMsg("editor.move-up")}\" \/><\/div>";
      var deleteButton = "<div class=\"vrtx-button vrtx-remove-button\"><input type=\"button\" value=\"${vrtx.getMsg("editor.remove")}\" \/><\/div>";
      var id = "<input type=\"hidden\" class=\"id\" value=\"" + counter + "\" \/>";
      var newElementId = "vrtx-json-element-" + j.name + "-" + counter;
    
      $("#" + j.name + " .vrtx-add-button").before("<div class=\"vrtx-json-element\" id=\"" + newElementId + "\"><\/div>");
    
      var newElement = $("#" + newElementId);
      newElement.append(htmlTemplate);
      newElement.append(id);
    
      if (counter > 0 && newElement.prev(".vrtx-json-element").length) {
        newElement.prev(".vrtx-json-element").append(moveDownButton);
      }
      newElement.append(deleteButton);
    
      if (counter > 0) {
        newElement.append(moveUpButton);
      }
      newElement.find(".vrtx-remove-button").click(function () {
        removeNode(j.name, counter, arrayOfIds);
      });
      newElement.find(".vrtx-move-up-button").click(function () {
        swapContent(counter, arrayOfIds, -1, j.name);
      });

      if (newElement.prev(".vrtx-json-element").length) {
        newElement.prev(".vrtx-json-element").find(".vrtx-move-down-button").click(function () {
          swapContent(counter-1, arrayOfIds, 1, j.name);
        });
      }

      // CK and date inputfields

      for (i in j.a) {
        var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
        if (j.a[i].type == "simple_html") {
          newEditor(inputFieldName, false, false, '${resourceContext.parentURI?js_string}', '${fckeditorBase.url?html}', 
                                                  '${fckeditorBase.documentURL?html}', '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', "");
        } else if (j.a[i].type == "html") {
          newEditor(inputFieldName, true, false, '${resourceContext.parentURI?js_string}', '${fckeditorBase.url?html}', 
                                                 '${fckeditorBase.documentURL?html}', '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', "");
        } else if (j.a[i].type == "datetime") {
          displayDateAsMultipleInputFields(inputFieldName);
        }
      }
    }
    
    function removeNode(name, counter, arrayOfIds) {
      var removeElementId = '#vrtx-json-element-' + name + '-' + counter;
      var removeElement = $(removeElementId);
      var siblingElement;
      if (removeElement.prev(".vrtx-json-element").length) {
        siblingElement = removeElement.prev(".vrtx-json-element");
      } else if (removeElement.next(".vrtx-json-element").length) {
        siblingElement = removeElement.next(".vrtx-json-element");
      }
      $(removeElementId + " textarea").each(function () {
        if (isCkEditor(this.name)) {
          getCkInstance(this.name).destroy();
        }
      });
      $(removeElementId).remove();
      removeUnwantedButtons(siblingElement);
    }
    
    function removeUnwantedButtons(siblingElement) {
      if (siblingElement) {
        var e = siblingElement.parents(".vrtx-json").find(".vrtx-json-element");
        while (e.prev(".vrtx-json-element").length) {
          e = e.prev(".vrtx-json-element");
        }
        e.find(".vrtx-move-up-button").remove();
        while (e.next(".vrtx-json-element").length) {
          e = e.next(".vrtx-json-element");
        }
        e.find(".vrtx-move-down-button").remove();
      }
    }
    
    // We need some way to add HTML from vrtx-types instead of building markup in JS
    
    function addDropdown(elem, inputFieldName) {
      var classes = "vrtx-string" + " " + elem.name;
      htmlTemplate = '<div class=\"' + classes + '\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<div class=\"inputfield\">';
      htmlTemplate += '<select id=\"' + inputFieldName + '\" name=\"' + inputFieldName + '\">';
      for (i in elem.valuemap) {
        var keyValuePair = elem.valuemap[i];
        var key = keyValuePair.split("$")[0];
        var value = keyValuePair.split("$")[1];
        htmlTemplate += '<option value=\"' + key + '\">' + value + '<\/option>';
      }
      htmlTemplate += '<\/select>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addStringField(elem, inputFieldName) {
      var classes = "vrtx-string" + " " + elem.name;
      htmlTemplate = '<div class=\"' + classes + '\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<div class=\"inputfield vrtx-textfield\">';
      htmlTemplate += '<input size=\"40\" type=\"text\" name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '\" \/>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addHtmlField(elem, inputFieldName) {
      var baseclass = "vrtx-html";
      if (elem.type == "simple_html") {
        baseclass = "vrtx-simple-html";
      }
      var classes = baseclass + " " + elem.name;
      htmlTemplate = '<div class=\"' + classes + '\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<textarea name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '\" ';
      htmlTemplate += ' rows=\"7\" cols=\"60\" ><\/textarea>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addBooleanField(elem, inputFieldName) {
      htmlTemplate = '<div class=\"vrtx-radio\">';
      htmlTemplate += '<div><label>elem.title<\/label><\/div>';
      htmlTemplate += '<input name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '-true\" type=\"radio\" value=\"true\" \/>';
      htmlTemplate += '<label for=\"' + inputFieldName + '-true\">True<\/label>';
      htmlTemplate += '<input name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '-false\" type=\"radio\" value=\"false\" \/>';
      htmlTemplate += '<label for=\"' + inputFieldName + '-false\">False<\/label>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addImageRef(elem, inputFieldName) {
      htmlTemplate = '<div class=\"vrtx-image-ref\">';
      htmlTemplate += '<div class=\"vrtx-image-ref-label\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<div class=\"vrtx-image-ref-browse\">';
      htmlTemplate += '<div class=\"vrtx-textfield\"><input type=\"text\" id=\"' + inputFieldName + '\" name=\"' + inputFieldName + '\" value=\"\" onblur=\"previewImage(\'' + inputFieldName + '\');\" size=\"30\" \/><\/div>';
      htmlTemplate += '<div class=\"vrtx-button\"><button type=\"button\" onclick=\"browseServer(\'' + inputFieldName + '\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI?js_string}\', \'${fckBrowse.url.pathRepresentation}\');\"><@vrtx.msg code="editor.browseImages" /><\/button><\/div>';
      htmlTemplate += '<div id=\"' + inputFieldName + '.preview\"><\/div>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addResourceRef(elem, inputFieldName) {
      htmlTemplate = '<div class=\"vrtx-resource-ref-browse\">';
      htmlTemplate += '<div class=\"vrtx-resource-ref-browse-label\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<div class=\"vrtx-resource-ref-browse-browse\">';
      htmlTemplate += '<div class=\"vrtx-textfield\"><input type=\"text\" name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '\" value=\"\" size=\"40\" \/><\/div>';
      htmlTemplate += '<div class=\"vrtx-button\"><button type=\"button\" onclick=\"browseServer(\'' + inputFieldName + '\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI?js_string}\', \'${fckBrowse.url.pathRepresentation}\',\'File\');\"><@vrtx.msg code="editor.browseImages" /><\/button><\/div>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addDateField(elem, inputFieldName) {
      htmlTemplate = '<div class=\"vrtx-string\">';
      htmlTemplate += '<label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<div class=\"inputfield vrtx-textfield\">';
      htmlTemplate += '<input size=\"20\" type=\"text\" name=\"' + inputFieldName + '\" id=\"' + inputFieldName + '\" value=\"\" class=\"date\" \/>';
      htmlTemplate += '<\/div>';
      htmlTemplate += '<\/div>';
      return htmlTemplate;
    }
    
    function addMediaRef(elem, inputFieldName) {
      htmlTemplate = '<div class=\"vrtx-media-ref\">';
      htmlTemplate += '<div><label for=\"' + inputFieldName + '\">' + elem.title + '<\/label>';
      htmlTemplate += '<\/div><div>';
      htmlTemplate += '<div class=\"vrtx-textfield\"><input type=\"text\" id=\"' + inputFieldName + '\" name=\"' + inputFieldName + '\" value=\"\" size=\"30\"\/><\/div>';
      htmlTemplate += '<div class=\"vrtx-button\"><button type=\"button\" onclick=\"browseServer(\'' + inputFieldName + '\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI?js_string}\', \'${fckBrowse.url.pathRepresentation}\', \'Media\');\"><@vrtx.msg code="editor.browseImages" /><\/button><\/div>';
      htmlTemplate += '<\/div><\/div>'
      return htmlTemplate;
    }
    
    // When move up or move down (+ scroll to)
    
    function swapContent(counter, arrayOfIds, move, name) {
      var thisId = "#vrtx-json-element-" + name + "-" + counter;
      var movedId = "#";
      if (move > 0) {
        movedId += $(thisId).next(".vrtx-json-element").attr("id");
      } else {
        movedId += $(thisId).prev(".vrtx-json-element").attr("id");
      }
      var arrayOfIdsLength = arrayOfIds.length;
      for (var x = 0; x < arrayOfIdsLength; x++) {
        var elementId1 = '#' + arrayOfIds[x] + counter;
        var moveToId;
        if (move > 0) {
          moveToId = parseInt($(elementId1).parents(".vrtx-json-element").next(".vrtx-json-element").find("input.id").val());
        } else {
          moveToId = parseInt($(elementId1).parents(".vrtx-json-element").prev(".vrtx-json-element").find("input.id").val());
        }
        var elementId2 = '#' + arrayOfIds[x] + moveToId;
        
        /* We need to handle special cases like date and CK fields  */
        var ckInstanceName1 = arrayOfIds[x].replace(/\\/g, '') + counter;
        var ckInstanceName2 = arrayOfIds[x].replace(/\\/g, '') + moveToId;
        if (isCkEditor(ckInstanceName1) && isCkEditor(ckInstanceName2)) {
          var val1 = getCkValue(ckInstanceName1);
          var val2 = getCkValue(ckInstanceName2);
          setCkValue(ckInstanceName1, val2);
          setCkValue(ckInstanceName2, val1);
        } else if ($(elementId1).hasClass("date") && $(elementId2).hasClass("date")) {
          var date1 = $(elementId1 + '-date');
          var hours1 = $(elementId1 + '-hours');
          var minutes1 = $(elementId1 + '-minutes');
          var date2 = $(elementId2 + '-date');
          var hours2 = $(elementId2 + '-hours');
          var minutes2 = $(elementId2 + '-minutes');
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
        var element1 = $(elementId1);
        var element2 = $(elementId2);
        var val1 = element1.val();
        var val2 = element2.val();
        element1.val(val2);
        element2.val(val1);
        element1.blur();
        element2.blur();
        element1.change();
        element2.change();
      }
      element1.closest(".vrtx-json-element").focusout();
      element2.closest(".vrtx-json-element").focusout();
      
      var absPos = $(movedId).offset();
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
    }
  
  // -->
  </script>
</#macro>
