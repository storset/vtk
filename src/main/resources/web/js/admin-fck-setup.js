// Set up an fck editor

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath,
    defaultLanguage, cssFileList) {

  var completeEditor = completeEditor != null ? completeEditor : false;
  var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;

  var ck = CKEDITOR;
  ck.baseHref = baseUrl + "/";
  
  ck.config['DefaultLanguage'] = defaultLanguage;

  ck.config['customConfig'] = baseUrl + '/custom-ckconfig.js';
        
  ck.config.autoGrow_maxHeight = '400px';
  ck.config.autoGrow_minHeight = '40px';
  ck.config.height = '250px';
  ck.config.resize_enabled = true;        
        
  ck.config.extraPlugins = 'MediaEmbed';
        
  if (completeEditor) {
    ck.config.toolbar = 'Complete_article';
    ck.config.autoGrow_minHeight = '50px';  	
  } else if (withoutSubSuper) {
    ck.config.toolbar = 'Inline_S';  	
  	ck.config.height = '40px';
  	ck.config.resize_enabled = false;
  } else {
    ck.config.toolbar = 'Inline';
    ck.config.height = '40px';
    ck.config.resize_enabled = false;
  }

  // File browser
  ck.config.filebrowserLinkBrowseUrl  = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
  ck.config.filebrowserImageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
  ck.config.filebrowserFlashBrowseUrl = baseUrl + '/plugins/filemanager/browser/cddefault/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

  ck.config.LinkUpload = false;
  ck.config.ImageUpload = false;
  ck.config.FlashUpload = false;

  // Misc setup
  ck.config['FullPage'] = false;
  ck.config['ToolbarCanCollapse'] = false;
  ck.config['TabSpaces'] = 4;
  ck.config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';
  ck.config.EMailProtection = 'none';
  ck.config.DisableFFTableHandles = false;
  ck.config.ForcePasteAsPlainText = false;

  ck.config['SkinPath'] = ck.BasePath + 'editor/skins/silver/';
  ck.config.BaseHref = baseDocumentUrl;

  var cssFileList = new Array(
          "/vrtx/__vrtx/static-resources/themes/default/editor-container.css",
          "/vrtx/__vrtx/static-resources/themes/default/fck_editorarea.css");

      /* Fix for div contianer display in ie */
      var browser = navigator.userAgent;
      var ieversion = new Number(RegExp.$1)
      if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
        cssFileList[cssFileList.length] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
      }

      ck.config['EditorAreaCSS'] = cssFileList;

  	  ck.replace(name, ck.config);
}

function FCKeditor_OnComplete(editorInstance) {
  // Get around bug: http://dev.fckeditor.net/ticket/1482
  editorInstance.ResetIsDirty();
  if ('resource.content' == editorInstance.Name) {
    enableSubmit();
  }
}

function disableSubmit() {
  document.getElementById("saveButton").disabled = true;
  document.getElementById("saveAndViewButton").disabled = true;
  return true;
}

function enableSubmit() {
  document.getElementById("saveButton").disabled = false;
  document.getElementById("saveAndViewButton").disabled = false;
  return true;
}
