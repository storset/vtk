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

  //CKEditor configurations
  var completeEditorConfig = {
  	filebrowserBrowseUrl : linkBrowseUrl,
  	filebrowserImageBrowseUrl : imageBrowseUrl,
  	filebrowserFlashBrowseUrl : flashBrowseUrl, 
  	extraPlugins : 'MediaEmbed',
  	toolbarCanCollapse : false,
  	contentsCss : cssFileList,
      defaultLanguage : 'no',
      language : defaultLanguage,
      toolbar : [

              [ 'Source', 'PasteText', '-', 'Undo', 'Redo', '-', 'Replace',
                      'RemoveFormat', '-', 'Link', 'Unlink', 'Anchor',
                      'Image', 'CreateDiv', 'MediaEmbed', 'Table',
                      'HorizontalRule', 'SpecialChar' 
               ],['Format', 'Bold', 'Italic', 'Underline', 'Strike',
                      'Subscript', 'Superscript', 'NumberedList',
                      'BulletedList', 'Outdent', 'Indent', 'JustifyLeft',
                      'JustifyCenter', 'JustifyRight', 'TextColor',
                      'Maximize' ]

      ],
      resize_enabled : true,
      autoGrow_maxHeight : '400px',
      autoGrow_minHeight : '50px',
      stylesSet : [
              {
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
              } ]
  }

  var inlineEditorConfig = {
      filebrowserBrowseUrl : linkBrowseUrl,
      defaultLanguage : 'no',
      language : defaultLanguage,
      toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
              'Italic', 'Underline', 'Strike', 'Subscript', 'Superscript',
              'SpecialChar' ] ],
      resize_enabled : true,
		toolbarCanCollapse : false,		
      height : '40px',
      autoGrow_maxHeight : '400px',
      autoGrow_minHeight : '40px'

  }

  var withoutSubSuperEditorConfig = {
  	filebrowserBrowseUrl : linkBrowseUrl,
      defaultLanguage : 'no',
      language : defaultLanguage,
      toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
              'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
      resize_enabled : true,
      toolbarCanCollapse : false,
      height : '40px',
      autoGrow_maxHeight : '400px',
      autoGrow_minHeight : '40px'
  }

  var introductionEditorConfig = {
  	filebrowserBrowseUrl : linkBrowseUrl,
      defaultLanguage : 'no',
      language : defaultLanguage,
      toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
              'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
      resize_enabled : false,
   	  toolbarCanCollapse : false,
   	  height : '150px',
      autoGrow_maxHeight : '400px',
      autoGrow_minHeight : '40px'
  }

  var captionEditorConfig = {
  	filebrowserBrowseUrl : linkBrowseUrl,
      defaultLanguage : 'no',
      language : defaultLanguage,
      toolbar : [ [ 'Source', 'PasteText', 'Link', 'Unlink', 'Bold',
              'Italic', 'Underline', 'Strike', 'SpecialChar' ] ],
      resize_enabled : false,
      toolbarCanCollapse : false,        
      height : '93px',
      autoGrow_maxHeight : '400px',
      autoGrow_minHeight : '40px'
  }
  
  var completeEditor = completeEditor != null ? completeEditor : false;
  var withoutSubSuper = withoutSubSuper != null ? withoutSubSuper : false;

  if (name.indexOf("introduction") != -1) {
	  setCKEditorConfig(name, introductionEditorConfig);
  } else if (name.indexOf("caption") != -1) {
	  setCKEditorConfig(name, captionEditorConfig);
  } else if (completeEditor) {
	  setCKEditorConfig(name,  completeEditorConfig);
  } else if (withoutSubSuper) {
	  setCKEditorConfig(name,  inlineEditorConfig);
  } else {
	  setCKEditorConfig(name,  withoutSubSuperEditorConfig);
  }

}

function setCKEditorConfig(name, configuration) {
  CKEDITOR.replace(name,configuration);
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
	  var commentsEditorConfig = {
        	toolbar : [ [ 'Source','Bold',
                'Italic', 'Underline', 'Strike', 'NumberedList',
                        'BulletedList', 'Link', 'Unlink' ] ],
        	resize_enabled : true,
        	height : '150px',
        	autoGrow_maxHeight : '400px',
        	autoGrow_minHeight : '40px' 
    	}
	  setCKEditorConfig("comments-text", commentsEditorConfig)
}