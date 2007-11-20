<#ftl strip_whitespace=true>

<#--
  - File: fckeditor.ftl
  - 
  - Required model data:
  -  
  -  fckeditorBase.url
  -  fckSource.getURL
  -  fckCleanup.url
  -  fckBrowse.url
  -  
  - Optional model data:
  -
  -->
<#import "/lib/ping.ftl" as ping />
<#import "/spring.ftl" as spring />
<#import "/lib/vortikal.ftl" as vrtx />

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>Editor</title>
    <@ping.ping url=pingURL['url'] interval=300 />
    <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
    <script type="text/javascript"><!--
      function newEditor(name, completeEditor)
      {
        var completeEditor = completeEditor != null ? completeEditor : false; 
        var fck = new FCKeditor( name ) ;
        fck.BasePath = "${fckeditorBase.url?html}/";

         // The toolbar: JSON string
         if (completeEditor) {
          fck.Config['ToolbarSets'] = "( {'" + name + "' : [\
            ['PasteText','PasteWord','-','Undo','Redo','-','Replace','RemoveFormat','-','Link','Unlink','Anchor','Image','Flash','Table','Rule','SpecialChar'],\'/',\
            ['FontFormat','-','Bold','Italic','Underline','StrikeThrough','Subscript','Superscript','OrderedList','UnorderedList','Outdent','Indent','JustifyLeft','JustifyCenter','JustifyRight','TextColor','FitWindow']]} )";

         } else {
          fck.Config['ToolbarSets'] = "( {'" + name + "' : [\
            ['Link','Unlink', 'Bold','Italic','Underline','StrikeThrough','Subscript','Superscript']]} )";
         }
         fck.ToolbarSet = name;

         // File browser
         var baseFolder = "${resourceContext.parentURI?html}";
         fck.Config['LinkBrowserURL']  = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

         // Misc setup
         fck.Config['FullPage'] = false;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['FontFormats'] = 'p;h1;h2;h3;h4;h5;h6;pre' ;        

         fck.Config.DisableFFTableHandles = false;
	 fck.Config.ForcePasteAsPlainText = false;

         fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';

         fck.ReplaceTextarea();
      }
      // -->
    </script>

    <!-- Yahoo YUI library: -->
    <link rel="stylesheet" type="text/css" href="${yuiBase.url?html}/build/calendar/assets/skins/sam/calendar.css">
    <script type="text/javascript" src="${yuiBase.url?html}/build/yahoo-dom-event/yahoo-dom-event.js"></script>
    <script type="text/javascript" src="${yuiBase.url?html}/build/calendar/calendar-min.js"></script>

  </head>
  <body>
    <form class="editor" action="" method="POST">
     
      <div class="properties">
        <@handleProps />
      </div>

      <div class="html-content">
       <textarea name="resource.content" rows="8" cols="60" id="resource.content">${resource.bodyAsString?html}</textarea>
       <@fck 'resource.content' true />

       </div>
      <#-- div class="properties"></div -->

      <div class="save-cancel">
       <input type="submit" onClick="performSave();" name="save" value="Lagre">
       <input type="submit" onClick="performSave();" name="cancel" value="Avbryt">

        <#if tooltips?exists>
          <#list tooltips as tooltip>
           <div class="contextual-help"><a href="javascript:void(0);" onclick="javascript:open('${tooltip.url?html}', 'componentList', 'width=650,height=450,resizable=yes,right=0,top=0,screenX=0,screenY=0,scrollbars=yes');">
              <@vrtx.msg code=tooltip.messageKey default=tooltip.messageKey/>
            </a>
           </div>
          </#list>
        </#if>

      </div>
     </form>

    </body>
</html>

<#macro handleProps>
  <#--
    <script type="text/javascript">
      window.onbeforeunload = confirmExit;

      function propChange() {
        <#local keyNames = resource.contentProperties />
        <#list keyNames as propDef>
          <#local name = propDef.name />
          <#local value = resource.getValue(propDef) />
          if ('${value}' != document.getElementById('resource.${name}').value) {
            return true;
          }
        </#list>
        return false;
      }

      function confirmExit() {
        var contentChange = (propChange() || FCKeditorAPI.GetInstance('content').IsDirty());
        if (needToConfirm && contentChange) {
          return "You have unsaved changes. Are you sure you want to leave this page?";
        }
      }
    </script>
   -->
    <#local locale = springMacroRequestContext.getLocale() />
    <#local keys = resource.contentProperties />
    <#list keys as propDef>
      <#local localizedName = propDef.getLocalizedName(locale) />
      <#local name = propDef.name />
      <#local value = resource.getValue(propDef) />
      <#local type = propDef.type />
      <#local error = resource.getError(propDef)?default('') />
      
      
      <div class="${name}">
        <label for="resource.${name}">${localizedName}</label> 
      <#if error != ""><span class="error">${error}</span></#if> 

      <#if type = 'HTML'>
        <textarea id="resource.${name}" name="resource.${name}" rows="8" cols="60">${value?html}</textarea>
        <@fck 'resource.${name}' />

      <#elseif name = 'media-ref'><#-- XXX -->
        <input type="text" id="resource.${name}"  name="resource.${name}" value="${value?html}"> 
        <button type="button" onclick="browseServer('resource.${name}', 'Media');">Browse media files</button>
        
      <#elseif type = 'IMAGE_REF'>
        <script type="text/javascript"><!--
             var urlobj;
             var baseFolder = "${resourceContext.parentURI?html}";
             function browseServer(obj, type) {
                     urlobj = obj;
                     if (type) {
                        openServerBrowser('${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=' + type + '&Connector=${fckBrowse.url.pathRepresentation}',
                             screen.width * 0.7,
                             screen.height * 0.7 ) ;

                     } else {
                        openServerBrowser('${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}',
                             screen.width * 0.7,
                             screen.height * 0.7 ) ;
                     }
             }

             function openServerBrowser( url, width, height ) {
                     var iLeft = (screen.width  - width) / 2 ;
                     var iTop  = (screen.height - height) / 2 ;
                     var sOptions = "toolbar=no,status=no,resizable=yes,dependent=yes" ;
                     sOptions += ",width=" + width ;
                     sOptions += ",height=" + height ;
                     sOptions += ",left=" + iLeft ;
                     sOptions += ",top=" + iTop ;
                     var oWindow = window.open( url, "BrowseWindow", sOptions ) ;
             }

             // Callback from the FCKEditor image browser:
             function SetUrl( url, width, height, alt ) {
                     document.getElementById(urlobj).value = url ;
                     oWindow = null;
                     previewImage(urlobj);
             }

             function previewImage(urlobj) {
                     var previewobj = urlobj + '.preview';
                     if (document.getElementById(previewobj)) {
                        var url = document.getElementById(urlobj).value;
                        if (url) {
                            document.getElementById(previewobj).innerHTML = 
                            '<img src="' + url + '" width="100" height="100">';
                        } else {
                            document.getElementById(previewobj).innerHTML = '';
                        }
                     }
             } //-->
        </script>
        <input type="text" id="resource.${name}" onblur="previewImage(id);" name="resource.${name}" value="${value?html}"> 
        <button type="button" onclick="browseServer('resource.${name}');">Browse images</button>
        <div id="resource.${name}.preview">
          <#if value != ''>
            <img src="${value}" width="100" height="100" alt="">
          </#if>
        </div>
      <#elseif type = 'DATE'>

        <#local dateVal = value />

        <#if value != "">
          <#local d = resource.resource.getProperty(propDef) />

          <#local dateVal = d.getFormattedValue('yyyy-MM-dd HH:mm:ss', springMacroRequestContext.getLocale()) />
          <#local year = d.getDateValue()?string("yyyy") />
          <#local month = d.getDateValue()?string("MM") />
          <#local jsmonth = ((d.getDateValue()?string("MM"))?number - 1)?string />
          <#local date = d.getDateValue()?string("dd") />
        </#if>

        <#local uniqueName = 'cal_' + propDef_index />

        <input type="text" id="resource.${name}" name="resource.${name}" value="${dateVal}" onblur="YAHOO.resource.${uniqueName}.calendar.cal1.syncDates()">
        <a class="calendar" href="javascript:void(0);" onclick="${uniqueName}_toggle()"><span>cal</span></a>
        <div id="resource.${name}.calendar" class="yui-skin-sam"></div>

        <script type="text/javascript"><!--

          YAHOO.namespace("resource.${uniqueName}.calendar");
          var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
          if (!cal1) {
             cal1 = YAHOO.resource.${uniqueName}.calendar.cal1 = 
             new YAHOO.widget.Calendar("cal1", "resource.${name}.calendar");
          }

         <#if value != "">
           cal1.cfg.setProperty("selected", "${jsmonth}/${date}/${year}", false);
           cal1.cfg.setProperty("pagedate", "${jsmonth}/${year}", false);
         </#if>

          cal1.selectEvent.subscribe( function(type, dates) {
             var date = this._toDate(dates[0][0]);
             var year = date.getFullYear();
             var monthNumber = date.getMonth() + 1;
             var month = monthNumber < 10 ? '0' + monthNumber  : '' + monthNumber;
             var day = date.getDate(); if (day < 10 ) day = '0' + day;
             var dateStr =  year + '-' + month + '-' + day + ' 00:00:00';

             document.getElementById('resource.${name}').value = dateStr;
             ${uniqueName}_hide();
          }, cal1, true);


          cal1.syncDates = function() {
             var input = document.getElementById('resource.${name}').value;
             var regexp = /(\d+)\-(\d\d)-(\d\d)/;
             var match = regexp.exec(input);
             
             if (match) {
                var d = new Date();
                d.setFullYear(match[1]);
                d.setMonth(match[2]);
                d.setDate(match[3]);

                this.cfg.setProperty("selected", d.getMonth() + "/" + d.getDate() + "/" + d.getFullYear(), false);
                this.cfg.setProperty("pagedate", d.getMonth() + "/" + d.getFullYear(), false);
                this.render();
             }
          }

          var ${uniqueName}_hidden = true;

          function ${uniqueName}_show() {
             var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
             cal1.render();
             cal1.show();
             ${uniqueName}_hidden = false;
          }

          function ${uniqueName}_hide() {
             var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
             cal1.hide();
             ${uniqueName}_hidden = true;
          }

          function ${uniqueName}_toggle() {
             if (${uniqueName}_hidden) {
                ${uniqueName}_show();
             } else {
                ${uniqueName}_hide();
             }
          }

          //-->
        </script>
      <#else>
        <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="35">
      </#if>
    </div>
    </#list>


</#macro>

<#macro fck content completeEditor=false>
    <script type="text/javascript">
      var needToConfirm = true;

      newEditor('${content}', ${completeEditor?string});

    function performSave() {
      needToConfirm = false;
      var oEditor = FCKeditorAPI.GetInstance('${content}');
      var srcxhtml = oEditor.GetXHTML();
      // var title = document.getElementById("title");

      // Title
      <#-- // srcxhtml = srcxhtml.replace(/<title.*<\/title>/i, "<title>" + title.value + "</title>"); -->
      document.getElementById('${content}').value = srcxhtml;
    }  

    </script>

</#macro>
