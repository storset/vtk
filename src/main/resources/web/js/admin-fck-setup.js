var inlineToolbar = [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                        'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript',
                          'SpecialChar' ] ];
var withoutSubSuperToolbar = [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                       'Italic', 'Underline', 'Strike', 'SpecialChar' ] ];

var introductionCaptionToolbar = [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
                                     'Italic', 'Underline', 'Strike', 'SpecialChar' ] ];

var completeToolbar = [ [ 'Source', 'PasteText', '-', 'Undo', 'Redo', '-', 'Replace',
                          'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                          'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                          'HorizontalRule', 'SpecialChar' 
                        ],['Format', 'Bold', 'Italic', 'Underline', 'Strike',
                           'Subscript', 'Superscript', 'NumberedList',
                           'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                           'JustifyCenter', 'JustifyRight', 'TextColor',
                           'Maximize' ]];

var commentsToolbar = [ [ 'Source','Bold',
                          'Italic', 'Underline', 'Strike', 'NumberedList',
                          'BulletedList', 'Link', 'Unlink' ] ];

function newEditor(name, completeEditor, withoutSubSuper, baseFolder, baseUrl, baseDocumentUrl, browsePath,
                   defaultLanguage, cssFileList) {

  // File browser
  var linkBrowseUrl  = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Connector=' + browsePath;
  var imageBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Image&Connector=' + browsePath;
  var flashBrowseUrl = baseUrl + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + '&Type=Flash&Connector=' + browsePath;

  /* Fix for div container display in IE */
  var browser = navigator.userAgent;
  var ieversion = new Number(RegExp.$1);
  if(browser.indexOf("MSIE") > -1 && ieversion <= 7){
    cssFileList.push("/vrtx/__vrtx/static-resources/themes/default/editor-container-ie.css");
  }
  
  var isCompleteEditor = completeEditor != null ? completeEditor : false;
  var isWithoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;
  
  var completeToolbarPointer = completeToolbar;  
  
  //CKEditor configurations
  if (name.indexOf("introduction") != -1) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, 
			          defaultLanguage, null, 150, 400, 40, introductionCaptionToolbar, isCompleteEditor, false);
  } else if (name.indexOf("caption") != -1) {
    setCKEditorConfig(name, linkBrowseUrl, null, null, 
			          defaultLanguage, null, 93, 400, 40, introductionCaptionToolbar, isCompleteEditor, false);
  } else if (isCompleteEditor) {	  
	var height = 220; var maxHeight = 400;
    if (name.indexOf("supervisor-box") != -1) { height = 130; maxHeight = 300; }
    else if (name.indexOf("content") != -1 && name != "related-content") { height = 400; maxHeight = 800; }
    
    console.log(name + " " + height + " " + maxHeight);
	
	setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, 
			          defaultLanguage, cssFileList, height ,maxHeight, 50, completeToolbarPointer, isCompleteEditor, true);
  } else if (isWithoutSubSuper) {
	setCKEditorConfig(name, linkBrowseUrl, null, null, 
			          defaultLanguage, null, 40, 400, 40, inlineToolbar, isCompleteEditor, true);
  } else {
	setCKEditorConfig(name, linkBrowseUrl, null, null,
			          defaultLanguage, null, 40, 400, 40, withoutSubSuperToolbar, isCompleteEditor, true);
  }

}

function setCKEditorConfig(name, linkBrowseUrl, imageBrowseUrl, flashBrowseUrl, defaultLanguage, cssFileList, height, maxHeight, minHeight, toolbar, complete, resizable) {
  var config = [{}];
  
  if(linkBrowseUrl != null) {
    config.filebrowserBrowseUrl = linkBrowseUrl;
  }
  
  if(complete) {
	config.filebrowserImageBrowseUrl = imageBrowseUrl;
	config.filebrowserFlashBrowseUrl = flashBrowseUrl;
	config.extraPlugins = 'MediaEmbed';
	config.contentsCss = cssFileList;
	var divContainerStylesSetPointer = divContainerStylesSet; // performance
    config.stylesSet = divContainerStylesSetPointer;
  }
  
  if(resizable) {
	config.resize_enabled = true;
  } else {
    config.resize_enabled = false;
  }	
  config.toolbarCanCollapse = false;
  config.defaultLanguage = 'no';
  if(defaultLanguage != null) {
    config.language = defaultLanguage;
  }
  config.toolbar = toolbar;
  config.height = height + 'px';
  config.autoGrow_maxHeight = maxHeight + 'px';
  config.autoGrow_minHeight = minHeight + 'px';
  
  CKEDITOR.replace(name, config);
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

function commentsCkEditor() { 
  setCKEditorConfig("comments-text", null, null, null, null, null, 150, 400, 40, commentsToolbar, false, true) 
}

var divContainerStylesSet = [{
    name : 'Facts left',
    element : 'div',
    attributes : {
        'class' : 'vrtx-facts-container vrtx-container-left'
    }
},
{
    name : 'Facts right',
    element : 'div',
    attributes : {
        'class' : 'vrtx-facts-container vrtx-container-right'
    }
},
{
    name : 'Image left',
    element : 'div',
    attributes : {
        'class' : 'vrtx-img-container vrtx-container-left'
    }
},
{
    name : 'Image center',
    element : 'div',
    attributes : {
        'class' : 'vrtx-img-container vrtx-container-middle vrtx-img-container-middle-ie'
    }
},
{
    name : 'Image right',
    element : 'div',
    attributes : {
        'class' : 'vrtx-img-container vrtx-container-right'
    }
},
{
    name : 'Img & capt left (800px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-left'
    }
},
{
    name : 'Img & capt left (700px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-left'
    }
},
{
    name : 'Img & capt left (600px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-left'
    }
},
{
    name : 'Img & capt left (500px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-left'
    }
},
{
    name : 'Img & capt left (400px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-left'
    }
},
{
    name : 'Img & capt left (300px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xs vrtx-container-left'
    }
},
{
    name : 'Img & capt left (200px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xxs vrtx-container-left'
    }
},
{
    name : 'Img & capt center (full)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-full vrtx-container-middle'
    }
},
{
    name : 'Img & capt center (800px)',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-middle'
    }
},
{
    name : 'Img & capt center (700px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-middle'
    }
},
{
    name : 'Img & capt center (600px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-middle'
    }
},
{
    name : 'Img & capt center (500px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-middle'
    }
},
{
    name : 'Img & capt center (400px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-middle'
    }
},
{
    name : 'Img & capt right (800px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xxl vrtx-container-right'
    }
},
{
    name : 'Img & capt right (700px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xl vrtx-container-right'
    }
},
{
    name : 'Img & capt right (600px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-l vrtx-container-right'
    }
},
{
    name : 'Img & capt right (500px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-m vrtx-container-right'
    }
},
{
    name : 'Img & capt right (400px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-s vrtx-container-right'
    }
},
{
    name : 'Img & capt right (300px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xs vrtx-container-right'
    }
},
{
    name : 'Img & capt right (200px) ',
    element : 'div',
    attributes : {
        'class' : 'vrtx-container vrtx-container-size-xxs vrtx-container-right'
    }
}];