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
        fck.Config['CustomConfigurationsPath'] = '${fckeditorBase.url?html}/custom-fckconfig.js';

         if (completeEditor) {
            fck.ToolbarSet = 'Complete';
         } else {
            fck.ToolbarSet = 'Inline';
         }

         // File browser
         var baseFolder = "${resourceContext.parentURI?html}";
         fck.Config['LinkBrowserURL']  = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['ImageBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
         fck.Config['FlashBrowserURL'] = '${fckeditorBase.url?html}/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

         // Misc setup
         fck.Config['FullPage'] = false;
         fck.Config['ToolbarCanCollapse'] = false;
         fck.Config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';

         fck.Config.DisableFFTableHandles = false;
	 fck.Config.ForcePasteAsPlainText = false;

         fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';

	 <#if fckEditorAreaCSSURL?exists>
           fck.Config['EditorAreaCSS'] = '${fckEditorAreaCSSURL[0]?html}';
	 </#if>

         fck.ReplaceTextarea();
      }
      function FCKeditor_OnComplete(editorInstance) {
          // Get around bug: http://dev.fckeditor.net/ticket/1482
          editorInstance.ResetIsDirty();
      }
      // -->
    </script>

    <!-- Yahoo YUI library: -->
    <link rel="stylesheet" type="text/css" href="${yuiBase.url?html}/build/calendar/assets/skins/sam/calendar.css">
    <script type="text/javascript" src="${yuiBase.url?html}/build/yahoo-dom-event/yahoo-dom-event.js"></script>
    <script type="text/javascript" src="${yuiBase.url?html}/build/calendar/calendar-min.js"></script>

  </head>
  <body>
    <#assign header>
      <@vrtx.msg code="editor.edit" args=[resource.resourceTypeDefinition.getLocalizedName(springMacroRequestContext.getLocale())?lower_case] />
    </#assign>
    <h2>${header}</h2>
    <form id="form" class="editor" action="" method="POST">
      <@handleProps />

      <div class="properties">
        <@propsForm resource.contentProperties />
      </div>

      <div class="html-content">
      <label for="resource.content"><@vrtx.msg code="editor.content" /></label> 
       <textarea name="resource.content" rows="8" cols="60" id="resource.content">${resource.bodyAsString?html}</textarea>

       <@fck 'resource.content' true />

       </div>

      <div class="properties">
        <@propsForm resource.extraContentProperties />
      </div>

      <div id="submit" class="save-cancel">
       <input type="submit" onClick="cSave();" name="save" value="${vrtx.getMsg("editor.save")}">
       <input type="submit" onClick="performSave();" name="savequit" value="${vrtx.getMsg("editor.saveAndQuit")}">
       <input type="submit" onClick="performSave();" name="cancel" value="${vrtx.getMsg("editor.cancel")}">
      </div>
     </form>
    </body>
</html>

<#macro propChangeTests propDefs>
  <#list propDefs as propDef>
    <#local name = propDef.name />
    <#local value = resource.getValue(propDef) />
    <#local type = propDef.type />
    <#if type = 'HTML'>
      if (FCKeditorAPI.GetInstance('resource.${name}').IsDirty()) {
        return true;
      }
    <#elseif type = 'DATE' || type = 'TIMESTAMP'>
      <#local dateVal = value />
      <#local hours = "" />
      <#local minutes = "" />
      <#if value != "">
        <#local d = resource.getProperty(propDef) />
        
        <#local dateVal = d.getFormattedValue('yyyy-MM-dd', springMacroRequestContext.getLocale()) />
        <#local hours = d.getFormattedValue('HH', springMacroRequestContext.getLocale()) />
        <#local minutes = d.getFormattedValue('mm', springMacroRequestContext.getLocale()) />
        <#if hours = "00" && minutes = "00">
          <#local hours = "" />
          <#local minutes = "" />
        </#if>
      </#if>

      if ('${dateVal}' != document.getElementById('resource.${name}').value) {
        return true;
      }
      if ('${hours}' != document.getElementById('resource.${name}.hours').value) {
        return true;
      }
      if ('${minutes}' != document.getElementById('resource.${name}.minutes').value) {
        return true;
      }            
    <#else>
      if ('${value}' != document.getElementById('resource.${name}').value) {
        return true;
      }
    </#if>
  </#list>
</#macro>

<#macro handleProps>
  <script type="text/javascript">
    function propChange() {
      <@propChangeTests resource.contentProperties />
      <@propChangeTests resource.extraContentProperties />
      return false;
    }

  <#--assign url = unlockURL['url']?default("") />
  <#if url != "">
    function doUnlock(event) {
      if (needToConfirm) {
        var req;
        if (window.XMLHttpRequest) {
          req = new XMLHttpRequest();
        } else if (window.ActiveXObject) {
          req = new ActiveXObject("Microsoft.XMLHTTP");
        }
        var url = '${url}';
        if (req != null) {
          req.open('GET', url, false);
          req.send(null);
        }
      }
    }

    window.onunload = doUnlock;
   </#if-->

    function doConfirm(event) {
      if (!needToConfirm) {
        return;
      }
      var contentChange = (propChange() || FCKeditorAPI.GetInstance('resource.content').IsDirty());
      if (contentChange) {
        return 'You have unsaved changes. Are you sure you want to leave this page?';
      }
    }

    window.onbeforeunload = doConfirm;

  </script>
</#macro>

<#macro propsForm propDefs>
    <#local locale = springMacroRequestContext.getLocale() />
    <#list propDefs as propDef>
      <#local localizedName = propDef.getLocalizedName(locale) />
      <#local description = propDef.getDescription(locale)?default("") />
      <#local name = propDef.name />
      <#local value = resource.getValue(propDef) />
      <#local type = propDef.type />
      <#local error = resource.getError(propDef)?default('') />
      
      
      <div class="${name} property-item">
      <label for="resource.${name}">${localizedName}</label> 
      <#if type = 'HTML'>
        <textarea id="resource.${name}" name="resource.${name}" rows="4" cols="60">${value?html}</textarea>
        <@fck 'resource.${name}' />

      <#elseif name = 'media'><#-- XXX -->
        <input type="text" id="resource.${name}"  name="resource.${name}" value="${value?html}"> 
        <button type="button" onclick="browseServer('resource.${name}', 'Media');"><@vrtx.msg code="editor.browseMediaFiles"/></button>
        
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
                            '<img src="' + url + '" alt="preview">';
                        } else {
                            document.getElementById(previewobj).innerHTML = '';
                        }
                     }
             } //-->
        </script>
        <input type="text" id="resource.${name}" onblur="previewImage(id);" name="resource.${name}" value="${value?html}"> 
        <script type="text/javascript" language="Javascript"><!--
        document.write('<button type="button" onclick="browseServer(\'resource.${name}\');"><@vrtx.msg code="editor.browseImages"/></button>');
        document.write('<div id="resource.${name}.preview">');
          <#if value != ''>
            document.write('<img src="${value}"  alt="preview">');
          </#if>
        document.write('</div>');
        // -->
        </script>
      <#elseif type = 'DATE' || type = 'TIMESTAMP'>

        <#local dateVal = value />
        <#local hours = "" />
        <#local minutes = "" />
        <#if value != "">
          <#local d = resource.getProperty(propDef) />

          <#local dateVal = d.getFormattedValue('yyyy-MM-dd', springMacroRequestContext.getLocale()) />
          <#local year = d.getDateValue()?string("yyyy") />
          <#local month = d.getDateValue()?string("MM") />
          <#local date = d.getDateValue()?string("dd") />
          <#local hours = d.getFormattedValue('HH', springMacroRequestContext.getLocale()) />
          <#local minutes = d.getFormattedValue('mm', springMacroRequestContext.getLocale()) />
          <#if hours = "00" && minutes = "00">
            <#local hours = "" />
            <#local minutes = "" />
          </#if>
        </#if>

        <#local uniqueName = 'cal_' + propDef_index />

        <input size="10" maxlength="10" type="text" class="date" id="resource.${name}" name="resource.${name}.date" value="${dateVal}" onblur="YAHOO.resource.${uniqueName}.calendar.cal1.syncDates()">
        <script type="text/javascript" language="Javascript"><!--
        document.write('<a class="calendar" id="${uniqueName}.calendar.href"><span>cal</span></a>');
        document.write('<div id="resource.${name}.calendar" class="yui-skin-sam"></div>');
        // -->
        </script>
        <input size="2" maxlength="2" type="text" class="hours" id="resource.${name}.hours" name="resource.${name}.hours" value="${hours}"><span class="colon">:</span><input size="2" maxlength="2" type="text" class="minutes" id="resource.${name}.minutes" name="resource.${name}.minutes" value="${minutes}">

        <script type="text/javascript" language="Javascript"><!--

          YAHOO.namespace("resource.${uniqueName}.calendar");
          var cal1 = YAHOO.resource.${uniqueName}.calendar.cal1;
          if (!cal1) {
             cal1 = YAHOO.resource.${uniqueName}.calendar.cal1 = 
             new YAHOO.widget.Calendar("cal1", "resource.${name}.calendar");
          }

         <#if value != "">
           cal1.cfg.setProperty("selected", "${month}/${date}/${year}", false);
           cal1.cfg.setProperty("pagedate", "${month}/${year}", false);
         </#if>
         cal1.render();

          cal1.selectEvent.subscribe( function(type, dates) {
             var date = this._toDate(dates[0][0]);
             var year = date.getFullYear();
             var monthNumber = date.getMonth() + 1;
             var month = monthNumber < 10 ? '0' + monthNumber  : '' + monthNumber;
             var day = date.getDate(); if (day < 10 ) day = '0' + day;
             var dateStr =  year + '-' + month + '-' + day;

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

          function ${uniqueName}_click(e) {
              if (!e) var e = window.event;
              var target;
              if (e.target) {
                 target = e.target;
              } else if (e.srcElement) {
                 target = e.srcElement;
              }
              var inCalendar = "${uniqueName}.calendar.href" == target.id;
              if (!${uniqueName}_hidden && !inCalendar) {
                 ${uniqueName}_hide();
              } else if (inCalendar) {
                 ${uniqueName}_toggle();
              }
              return true;
          }

          if (window.addEventListener) {
             window.addEventListener("click", ${uniqueName}_click, false);
          } else if (window.attachEvent) {
             document.attachEvent('onclick', ${uniqueName}_click);
          }
          //-->
        </script>
      <#else>
        <input type="text" id="resource.${name}" name="resource.${name}" value="${value?html}" size="32">
        <#if description != "">
          <span class="input-description">(${description})</span>
        </#if>
      </#if>
      <#if error != ""><span class="error">${error}</span></#if> 

    </div>
    </#list>
</#macro>

<#macro fck content completeEditor=false>
    <script type="text/javascript">
      var needToConfirm = true;
      newEditor('${content}', ${completeEditor?string});
      

    function cSave() {
      document.getElementById("form").setAttribute("action", "#submit");
      performSave();
    }

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
