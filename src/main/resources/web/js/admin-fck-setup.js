// Set up an fck editor

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath,
    defaultLanguage, cssFileList) {

  var completeEditor = completeEditor != null ? completeEditor : false;
  var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;

  var fck = new FCKeditor(name, '100%', 400);
  fck.BasePath = baseUrl + "/";

  fck.Config['DefaultLanguage'] = defaultLanguage;

  fck.Config['CustomConfigurationsPath'] = baseUrl + '/custom-fckconfig.js';

  if (completeEditor) {
    fck.ToolbarSet = 'Complete';
  } else if (withoutSubSuper) {
    fck.ToolbarSet = 'Inline-S';
  } else {
    fck.ToolbarSet = 'Inline';
  }

  // File browser
  fck.Config['LinkBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
      + '&Connector=' + browsePath;
  fck.Config['ImageBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
      + '&Type=Image&Connector=' + browsePath;
  fck.Config['FlashBrowserURL'] = baseUrl + '/editor/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder
      + '&Type=Flash&Connector=' + browsePath;

  fck.Config.LinkUpload = false;
  fck.Config.ImageUpload = false;
  fck.Config.FlashUpload = false;

  // Misc setup
  fck.Config['FullPage'] = false;
  fck.Config['ToolbarCanCollapse'] = false;
  fck.Config['TabSpaces'] = 4;

  fck.Config['FontFormats'] = 'p;h2;h3;h4;h5;h6;pre';

  fck.Config.EMailProtection = 'none';
  fck.Config.DisableFFTableHandles = false;
  fck.Config.ForcePasteAsPlainText = false;

  fck.Config['SkinPath'] = fck.BasePath + 'editor/skins/silver/';
  fck.Config.BaseHref = baseDocumentUrl;

  /* Fix for div contianer display in ie */
  var browser = navigator.userAgent;
  var ieversion = new Number(RegExp.$1)
  if (browser.indexOf("MSIE") > -1 && ieversion <= 7) {
    cssFileList[cssFileList.length - 1] = "/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css";
  }

  fck.Config['EditorAreaCSS'] = cssFileList;
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
  document.getElementById("saveAndQuitButton").disabled = true;
  return true;
}

function enableSubmit() {
  document.getElementById("saveButton").disabled = false;
  document.getElementById("saveAndQuitButton").disabled = false;
  return true;
}
