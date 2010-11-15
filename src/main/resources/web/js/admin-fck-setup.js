// Set up an fck editor

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath,
    defaultLanguage, cssFileList) {

  var completeEditor = completeEditor != null ? completeEditor : false;
  var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;

  var fck = new CKEDITOR;
  fck.BasePath = baseUrl + "/";

  fck.config['DefaultLanguage'] = defaultLanguage;

  fck.config['CustomConfigurationsPath'] = baseUrl + '/custom-fckconfig.js';

  if (completeEditor) {
    fck.ToolbarSet = 'Complete-article';
  } else if (withoutSubSuper) {
    fck.ToolbarSet = 'Inline-S';
  } else {
    fck.ToolbarSet = 'Inline';
  }

  // File browser
  // fck.config['LinkBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
  //     + '&Connector=' + browsePath;
  // fck.config['ImageBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
  //     + '&Type=Image&Connector=' + browsePath;
  // fck.config['FlashBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
  //    + '&Type=Flash&Connector=' + browsePath;

  fck.config.filebrowserLinkBrowseURL  = '${fckeditorBase.url?html}/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=${fckBrowse.url.pathRepresentation}';
  fck.config.filebrowserImageBrowseURL = '${fckeditorBase.url?html}/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=${fckBrowse.url.pathRepresentation}';
  fck.config.filebrowserFlashBrowseURL = '${fckeditorBase.url?html}/plugins/filemanager/browser/cddefault/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=${fckBrowse.url.pathRepresentation}';

  fck.config.LinkUpload = false;
  fck.config.ImageUpload = false;
  fck.config.FlashUpload = false;

  // Misc setup
  fck.config['FullPage'] = false;
  fck.config['ToolbarCanCollapse'] = false;
  fck.config['TabSpaces'] = 4;
  fck.config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';
  fck.config.EMailProtection = 'none';
  fck.config.DisableFFTableHandles = false;
  fck.config.ForcePasteAsPlainText = false;

  fck.config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';
  fck.config.BaseHref = baseDocumentUrl;

  var cssFileList = new Array(
          "/vrtx/__vrtx/static-resources/themes/default/editor-container.css",
          "/vrtx/__vrtx/static-resources/themes/default/fck_editorarea.css");

      /* Fix for div contianer display in ie */
      var browser = navigator.userAgent;
      var ieversion = new Number(RegExp.$1)
      if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
        cssFileList[cssFileList.length] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
      }

      fck.config['EditorAreaCSS'] = cssFileList;

  fck.ReplaceTextarea();
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
