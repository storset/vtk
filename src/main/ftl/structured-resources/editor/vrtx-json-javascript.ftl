<#-- 
  Evil hack(s) alert! 
-->
<#macro script >
  <#assign locale = springMacroRequestContext.getLocale() />
  <script language="Javascript" type="text/javascript">
  
  LIST_OF_JSON_ELEMENTS = new Array();
  $(document).ready(function() {   
  
  <#assign i = 0 />
  <#list form.elements as elementBox>
    <#assign j = 0 />
    <#list elementBox.formElements as elem>
      <#if elem.description.type == "json" && elem.description.isMultiple() >
      LIST_OF_JSON_ELEMENTS[${i}] = new Object();
      LIST_OF_JSON_ELEMENTS[${i}].name = "${elem.name}";
      LIST_OF_JSON_ELEMENTS[${i}].type = "${elem.description.type}";
      LIST_OF_JSON_ELEMENTS[${i}].a = new Array();

    <#list elem.description.attributes as jsonAttr>
      LIST_OF_JSON_ELEMENTS[${i}].a[${j}] = new Object();
      LIST_OF_JSON_ELEMENTS[${i}].a[${j}].name = "${jsonAttr.name}";
      LIST_OF_JSON_ELEMENTS[${i}].a[${j}].type = "${jsonAttr.type}";
      LIST_OF_JSON_ELEMENTS[${i}].a[${j}].title = "${form.resource.getLocalizedMsg(jsonAttr.name, locale, null)}";
      <#assign j = j + 1 />
    </#list>
      <#assign i = i + 1 />
       </#if>
    </#list>
  </#list>
    for(i in LIST_OF_JSON_ELEMENTS){
        $("#" + LIST_OF_JSON_ELEMENTS[i].name).append("<input type=\"button\" class=\"vrtx-add-button\" onClick=\"addNewJsonElement(LIST_OF_JSON_ELEMENTS[" + i + "])\" value=\"${vrtx.getMsg("editor.add")}\" />");
      }
    });
    
    function getCounterForJson(inputFieldName, jsonAttr){
      var i = 0;
      while( $("#" + inputFieldName + "\\." + jsonAttr + "\\." + i).size() > 0 ){   
        i++;
      }
      return i;
    }
    
  function addNewJsonElement(j){
  
     var counter = getCounterForJson(j.name, j.a[0].name);
     
     // Add opp og ned knapp...blah
     
     var htmlTemplate = "";
     var arrayOfIds = "new Array("
     for(i in j.a){
         var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
         if(i > 0){
           arrayOfIds += "," 
         }
         arrayOfIds += "'" + j.name + "." + j.a[i].name + "." + "'";
         
         switch(j.a[i].type) {
           case "string":
             htmlTemplate += addStringField(j.a[i],inputFieldName); break
           case "html":
             htmlTemplate += addHtmlField(j.a[i],inputFieldName); break
           case "simple_html":
             htmlTemplate += addHtmlField(j.a[i],inputFieldName); break
           case "boolean":
             htmlTemplate += addBooleanField(j.a[i],inputFieldName); break
           case "image_ref":
             htmlTemplate += addImageRef(j.a[i],inputFieldName); break
           case "datetime":
             htmlTemplate += addDateField(j.a[i],inputFieldName); break
           case "media":
             htmlTemplate += addMediaRef(j.a[i],inputFieldName); break
           default:
             htmlTemplate += ""; break
         }
     }
     arrayOfIds = arrayOfIds.replace(/\./g,"\\\\.");
     
     // Need a move down button for the element before the element we are inserting 
     lastElement = "vrtx-" + j.type + "-element-" + j.name + "-" + (counter-1);
     var moveDownButton = "<input type=\"button\" class=\"vrtx-move-down-button\" value=\"&darr; ${vrtx.getMsg("editor.move-down")}\" onClick=\"swapContent(" + (counter-1) + "," + arrayOfIds.toString() + "),1)\" />";
     $("#" + lastElement).append(moveDownButton);
     
     //The new element needs a move up button and also a delete button
     var moveUpButton = "<input type=\"button\" class=\"vrtx-move-up-button\" value=\"&uarr; ${vrtx.getMsg("editor.move-up")}\" onClick=\"swapContent(" + counter + "," + arrayOfIds.toString() + "),-1)\" />";
     var deleteButton = "<input type=\"button\" class=\"vrtx-remove-button\" value=\"${vrtx.getMsg("editor.remove")}\" onClick=\"$('#vrtx-json-element-" + j.name + "-" + counter + "').remove()\" \/>";
     $("#" + j.name +" .vrtx-add-button").before("<div class=\"vrtx-json-element\" id=\"vrtx-json-element-" + j.name + "-" + counter + "\">" +  htmlTemplate + deleteButton + moveUpButton + "<\/div>");
     
     // Fck.........
     for(i in j.a){
       var inputFieldName = j.name + "." + j.a[i].name + "." + counter;
       if(j.a[i].type == "simple_html"){  
         newEditor(inputFieldName,false,false,'${resourceContext.parentURI}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
          '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', '');
       }else if(j.a[i].type == "html"){
         newEditor(inputFieldName,true,false,'${resourceContext.parentURI}', '${fckeditorBase.url?html}', '${fckeditorBase.documentURL?html}', 
          '${fckBrowse.url.pathRepresentation}', '<@vrtx.requestLanguage />', '');
       }else if(j.a[i].type == "datetime"){
      displayDateAsMultipleInputFields(inputFieldName);
     }
     
    }
  }
  

  function addStringField(elem,inputFieldName){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
    htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
    htmlTemplate += '<div class=\"inputfield\">';
    htmlTemplate += '<input size=\"{inputFieldSize}\" type=\"text\" name=\"{inputFieldName}\" id=\"{inputFieldName}\" />';
    htmlTemplate +=  '<\/div>';
    htmlTemplate +=  '<div class=\"tooltip\">{tooltip}<\/div>';
    htmlTemplate +=  '<\/div>';
    
    return processHtmlTemplate(elem.title,"vrtx-string" + " " + elem.name,htmlTemplate,inputFieldName);
  }
  
  function addHtmlField(elem,inputFieldName,counter){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
        htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
        htmlTemplate += '<div>';
        htmlTemplate += '<textarea name=\"{inputFieldName}\" id=\"{inputFieldName}\" ';
        htmlTemplate += ' rows=\"4\" cols=\"20\" >{value}<\/textarea>';
        htmlTemplate += '<\/div><\/div>';
    
    if(elem.type == "simple_html"){
      var classes = "vrtx-simple-html";
    }else{
      var classes = "vrtx-html";
    }
    
    return processHtmlTemplate(elem.title,classes,htmlTemplate,inputFieldName);
  }
  
  function addBooleanField(elem,inputFieldName,counter){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
      htmlTemplate += '<div><label>{title}<\/label><\/div>';
      htmlTemplate += '<div>';
        htmlTemplate += '<input name=\"{inputFieldName}\" id=\"{inputFieldName}-true\" type=\"radio\" value=\"true\"  \/>';
        htmlTemplate += '<label for=\"{inputFieldName}-true\">True<\/label> ';
        htmlTemplate += '<input name=\"{inputFieldName}\" id=\"{inputFieldName}-false\" type=\"radio\" value=\"false\" \/>';
        htmlTemplate +=  '<label for=\"{inputFieldName}-false\">False<\/label>';
      htmlTemplate += '<\/div><\/div>';
      
      return processHtmlTemplate(elem.title,"vrtx-radio",htmlTemplate,inputFieldName);
  }
  
  function addImageRef(elem,inputFieldName,counter){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
    htmlTemplate += '<div>';
    htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
    htmlTemplate += '<\/div><div>';
      htmlTemplate += '<input type=\"text\" id=\"{inputFieldName}\" name=\"{inputFieldName}\" value=\"\" onblur=\"previewImage({inputFieldName});\" size=\"30\" \/>';
        htmlTemplate += '<button type=\"button\" onclick=\"browseServer(\'{inputFieldName}\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI}\',\'${fckBrowse.url.pathRepresentation}\');\"><@vrtx.msg code="editor.browseImages" /><\/button>';
    htmlTemplate += '<\/div>';
    htmlTemplate += '<div id=\"{inputFieldName}.preview\">';
    htmlTemplate += '<img src=\"{value}?vrtx=thumbnail\" alt=\"Preview image\" \/>';
    htmlTemplate += '<\/div><\/div>';
    
    return processHtmlTemplate(elem.title,"vrtx-image-ref",htmlTemplate,inputFieldName);
  }
  
  function addDateField(elem,inputFieldName,counter){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
    htmlTemplate += '<label for=\"{inputFieldName}\">{title}<\/label>';
    htmlTemplate += '<div class=\"inputfield\">';
    htmlTemplate += '<input size=\"20\" type=\"text\" name=\"{inputFieldName}\" id=\"{inputFieldName}\" value=\"{value}\" class=\"date\" \/>';
    htmlTemplate += '<\/div>';
    htmlTemplate += '<div class=\"tooltip\">{tooltip}<\/div>';
    htmlTemplate += '<\/div>';
    
    return processHtmlTemplate(elem.title,"vrtx-string date",htmlTemplate,inputFieldName);
  }

  function addMediaRef(elem,inputFieldName,counter){
    var htmlTemplate = new String();
    htmlTemplate = '<div class=\"{classes}\">';
    htmlTemplate += '<div><label for=\"{inputFieldName}\">{title}<\/label>';
    htmlTemplate += '<\/div><div>';
    htmlTemplate += '<input type=\"text\" id=\"{inputFieldName}\" name=\"{inputFieldName}\" value=\"{value}\" onblur=\"previewImage({inputFieldName});\" size=\"30\"\/>';
    htmlTemplate += '<button type=\"button\" onclick=\"browseServer(\'{inputFieldName}\', \'${fckeditorBase.url}\', \'${resourceContext.parentURI}\',\'${fckBrowse.url.pathRepresentation}\',\'Media\');\"><@vrtx.msg code="editor.browseImages" /><\/button>';
    htmlTemplate += '<\/div><div id=\"{inputFieldName}.preview\">';
    htmlTemplate += '<img src=\"\" \/>';
    htmlTemplate += '<\/div><\/div>'
    
    return processHtmlTemplate(elem.title,"vrtx-media-ref",htmlTemplate,inputFieldName);
  }

  function processHtmlTemplate(name,classes,template,inputFieldName){
    var htmlTemplate = new String(template);
    htmlTemplate = htmlTemplate.replace(/{inputFieldName}/g,inputFieldName);
    htmlTemplate = htmlTemplate.replace(/{inputFieldSize}/g,"40");
    htmlTemplate = htmlTemplate.replace(/{tooltip}/g,"");
    htmlTemplate = htmlTemplate.replace(/{title}/g,name);
    htmlTemplate = htmlTemplate.replace(/{classes}/g,classes);
    htmlTemplate = htmlTemplate.replace(/{value}/g,"");
    
    return htmlTemplate;
  }
  

  function getFckValue(instanceName){
    var oEditor = FCKeditorAPI.GetInstance(instanceName) ;
    return oEditor.GetXHTML(true) ;
  }
  
  function setFckValue(instanceName, data){
    var oEditor = FCKeditorAPI.GetInstance(instanceName) ;
    oEditor.SetData(data) ;
  }
   
  function isFckEditor(instanceName) {
    var oEditor = FCKeditorAPI.GetInstance(instanceName) ;
    return oEditor != null;
  }
  
  function swapContent(counter,arrayOfIds,move){  
    for(x in arrayOfIds){  
      var elementId1 = '#' + arrayOfIds[x] + counter;
      var elementId2 = '#' + arrayOfIds[x] + (counter + move);

      /* We need to handle special cases like date and fck fields  */
      var fckInstanceName1 = arrayOfIds[x].replace(/\\/g,'') + counter;
      var fckInstanceName2 = arrayOfIds[x].replace(/\\/g,'') + (counter + move);
      if(isFckEditor(fckInstanceName1) && isFckEditor(fckInstanceName2)){
        var val1 = getFckValue(fckInstanceName1);
        var val2 = getFckValue(fckInstanceName2);
        setFckValue(fckInstanceName1, val2);
        setFckValue(fckInstanceName2, val1);
      }else if($(elementId1).hasClass("date") && $(elementId2).hasClass("date")){    
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
    }
  }

  </script>
</#macro>
