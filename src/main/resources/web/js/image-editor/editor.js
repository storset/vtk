/* 
 * Vortex HTML5 Canvas image editor
 *
 */

function VrtxImageEditor() {
  var instance; // Class-like singleton pattern (p.145 JavaScript Patterns)
  VrtxImageEditor = function VrtxImageEditor() {
    return instance;
  };
  VrtxImageEditor.prototype = this;
  instance = new VrtxImageEditor();
  instance.constructor = VrtxImageEditor;
  
  this.url = null;
  this.img = null;
  this.scaledImg = null;
  
  this.canvasSupported = null;
  this.canvas = null;
  this.ctx = null;
  this.origw = null;
  this.origh = null;
  this.rw = null;
  this.rh = null;
  this.cropX = null;
  this.cropY = null;
  this.cropWidth = null;
  this.cropHeight = null;
  this.scaleRatio = null;
  this.reversedScaleRatio = null;
  this.aspectRatioOver = 1;
  this.aspectRatioUnder = 1;
  this.keepAspectRatio = true;
  this.hasCropBeenInitialized = false;

  return instance;
};

var vrtxImageEditor = new VrtxImageEditor();

VrtxImageEditor.prototype.init = function init(imageEditorElm, imgURL) {
  var editor = this;

  imageEditorElm.addClass("canvas-supported");
  
  editor.canvasSupported = 'getContext' in document.createElement('canvas');
  var $canvas = imageEditorElm.find("#vrtx-image-editor");
  
  if(editor.canvasSupported) {
    editor.canvas = $canvas[0];
    editor.ctx = editor.canvas.getContext('2d');
  }

  editor.img = new Image();
  editor.scaledImg = new Image();
  
    editor.img.src = imgURL;
  
  editor.img.onload = function () {
    editor.rw = editor.origw = editor.cropWidth = editor.img.width;
    editor.rh = editor.origh = editor.cropHeight = editor.img.height;
    
    if(!editor.canvasSupported) {
      $canvas.replaceWith("<img src='" + editor.url + "' alt='preview image' />");
      editor.displayDimensions(editor.rw, editor.rh);
      $("#resource-width").attr("disabled", "disabled");
      $("#resource-height").attr("disabled", "disabled");
    } else {
      var r = gcd (editor.rw, editor.rh);
      editor.aspectRatioOver = editor.rw/r;
      editor.aspectRatioUnder = editor.rh/r;
      editor.cropX = 0;
      editor.cropY = 0;
      editor.scaleRatio = 1;
      editor.reversedScaleRatio = 1;
    
      editor.canvas.setAttribute('width', editor.rw);
      editor.canvas.setAttribute('height', editor.rh);
      editor.canvas.width = editor.rw;
      editor.canvas.height = editor.rh;
      editor.displayDimensions(editor.rw, editor.rh);
      editor.ctx.drawImage(editor.img, 0, 0);
    
      editor.renderScaledImage(false); 
    
      $canvas.resizable({
        aspectRatio: editor.keepAspectRatio,
        grid: [1, 1],
        stop: function (event, ui) {
          var newWidth = Math.floor(ui.size.width);
          var newHeight = Math.floor(ui.size.height);
        
          var correctH = Math.round(newWidth / (editor.aspectRatioOver / editor.aspectRatioUnder));
        
          editor.scale(newWidth, correctH);
        },
        resize: function (event, ui) {
          editor.displayDimensions(Math.floor(ui.size.width), Math.floor(ui.size.height));
        }
      });
    }
  }

  if(editor.canvasSupported) {
    $("#app-content").delegate("#vrtx-image-editor", "dblclick", function (e) {
      $("#vrtx-image-crop").click();
    });
  
    $("#app-content").delegate("#vrtx-image-crop", "click", function (e) {
      if (editor.hasCropBeenInitialized) {
        editor.cropX += Math.round(theSelection.x * editor.reversedScaleRatio);
        editor.cropY += Math.round(theSelection.y * editor.reversedScaleRatio);
        editor.cropWidth = Math.round(theSelection.w * editor.reversedScaleRatio);
        editor.cropHeight = Math.round(theSelection.h * editor.reversedScaleRatio);
        editor.rw = Math.round(editor.cropWidth * editor.scaleRatio);
        editor.rh = Math.round(editor.cropHeight * editor.scaleRatio);

        var r = gcd (editor.rw, editor.rh);
        editor.aspectRatioOver = editor.rw/r;
        editor.aspectRatioUnder = editor.rh/r;
      
        editor.updateDimensions(editor.rw, editor.rh);

        editor.ctx.drawImage(editor.img, editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight, 
                                                    0,            0, editor.rw, editor.rh);
                                                                                     
        editor.renderScaledImage(false); 
        editor.resetCropPlugin();
        $(this).val("Start beskjæring...");
        $("#vrtx-image-editor").resizable("enable");
      
        editor.hasCropBeenInitialized = false;
      } else {
        initSelection(editor);
        $(this).val("Beskjær bilde");
        $("#vrtx-image-editor").resizable("disable");
      
        editor.hasCropBeenInitialized = true;
      }
      e.stopPropagation();
      e.preventDefault();
    });

    $("#app-content").delegate("#resource-width, #resource-height", "change", function (e) {
      var w = parseInt($.trim($("#resource-width").val()));
      var h = parseInt($.trim($("#resource-height").val()));
      if (!w.isNaN && !h.isNaN) {
        if (w !== editor.rw) {
          if (editor.keepAspectRatio) {
            h = Math.round(w / (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-height").val(h)
        } else if (h !== editor.rh) {
          if (editor.keepAspectRatio) {
            w = Math.round(h * (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-width").val(w)
        }
        editor.scale(w, h);
      }
    });

    $("#app-content").delegate("#resource-width", "keydown", function (e) {
      if (e.which == 38 || e.which == 40) {
        var w = parseInt($.trim($("#resource-width").val()));
        var h = parseInt($.trim($("#resource-height").val()));
        if (!w.isNaN && !h.isNaN) {
          if (e.which == 38) {
            w++;
          } else {
            if (w > 2) {
              w--;
            }
          }
          if (editor.keepAspectRatio) {
            h = Math.round(w / (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-width").val(w);
          $("#resource-height").val(h);
          editor.scale(w, h);
        }
      }
    });

    $("#app-content").delegate("#resource-height", "keydown", function (e) {
      if (e.which == 38 || e.which == 40) {
        var w = parseInt($.trim($("#resource-width").val()));
        var h = parseInt($.trim($("#resource-height").val()));
        if (!w.isNaN && !h.isNaN) {
          if (e.which == 38) {
            h++;
          } else {
            if (h > 2) {
              h--;
            }
          }
          if (editor.keepAspectRatio) {
            w = Math.round(h * (editor.aspectRatioOver / editor.aspectRatioUnder));
          }
          $("#resource-width").val(w);
          $("#resource-height").val(h);
          editor.scale(w, h);
        }
      }
    });
  
    $("#app-content").delegate("#saveAndViewButton", "click", function(e) {;
      if(!savedImage) {
        if(editor.hasCropBeenInitialized) {
          cropNone(editor);
        }
        if(editor.scaleRatio < 0.9) {
          editor.scaleLanczos(2); // http://int64.org/2011/07/24/choosing-the-right-kernel
        } else {
          editor.save();
        }
        return false; 
      } else {
        savedImage = false;
      }
    });
  
    $(document).click(function(e) {
      if(editor.hasCropBeenInitialized && $(e.target).parents().index($('#vrtx-image-editor-inner-wrapper')) == -1) {
        /*var posX = e.pageX; // http://docs.jquery.com/Tutorials:Mouse_Position
        var posY = e.pageY;
        var editorOffset = $("#vrtx-image-editor").offset();
        var editorX = Math.round(editorOffset.left + theSelection.x) - 15;
        var editorY = Math.round(editorOffset.top + theSelection.y) - 15;
        var editorW = Math.round(editorX + theSelection.w) + 15;
        var editorH = Math.round(editorY + theSelection.h) + 15;
        if(posX > editorX && posX < editorW && posY > editorY && posY < editorH) {
        } else {*/
          cropNone(editor);
        /* }*/
      }
    });
  }
};

function gcd (a, b) {
  return (b == 0) ? a : gcd (b, a%b);
}

function cropNone(editor) {
  theSelection.x = 0;
  theSelection.y = 0;
  theSelection.w = editor.rw;
  theSelection.h = editor.rh;
  $("#vrtx-image-crop").click();
}

var savedImage = false;
VrtxImageEditor.prototype.save = function save() {
  savedImage = true;

  var imageAsBase64 = vrtxImageEditor.canvas.toDataURL("image/png");
  imageAsBase64 = imageAsBase64.replace("data:image/png;base64,", "");
  var form = $("form#vrtx-image-editor-save-image-form");
  if("FormData" in window) { // If FormData is supported
    var fd = new FormData(); // Info: http://hacks.mozilla.org/2011/01/how-to-develop-a-html5-image-uploader/
                             //       http://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#interface-formdata
    fd.append("csrf-prevention-token", form.find("input[name=csrf-prevention-token]").val()); 
    fd.append("base", imageAsBase64);
    var xhr = new XMLHttpRequest();
    xhr.open("POST", form.attr("action"));
    xhr.send(fd);
    xhr.onreadystatechange = function() {
      if($.browser.mozilla) { // http://www.nczonline.net/blog/2009/07/09/firefox-35firebug-xmlhttprequest-and-readystatechange-bug/
        xhr.onload = xhr.onerror = xhr.onabort = function() {
          $("#saveAndViewButton").click();      
        };
      } else {
        if (xhr.readyState == 4)  { 
          $("#saveAndViewButton").click();
        }
      }
    };
  } else { // Otherwise try to append imageAsBase64 to form instead
    form.append("<input type='hidden' name='base' value='" + imageAsBase64 + "' />");
    form.submit();
  }
};

VrtxImageEditor.prototype.scale = function scale(newWidth, newHeight) {
  var editor = this;

  editor.scaleRatio = newWidth / editor.cropWidth;
  editor.reversedScaleRatio = editor.cropWidth / newWidth;
  editor.rw = newWidth;
  editor.rh = newHeight;
  editor.updateDimensions(editor.rw, editor.rh);
  editor.ctx.drawImage(editor.img, editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight, 
                                              0,            0, editor.rw, editor.rh);
  editor.renderScaledImage(false);      
};

VrtxImageEditor.prototype.resetCropPlugin = function resetCropPlugin() {
  $("#vrtx-image-editor").unbind("mousemove").unbind("mousedown").unbind("mouseup");
  iMouseX, iMouseY = 1;
  theSelection;
};

VrtxImageEditor.prototype.updateDimensions = function updateDimensions(w, h) {
  var editor = this;

  editor.canvas.setAttribute('width', w);
  editor.canvas.setAttribute('height', h);
  editor.canvas.width = w;
  editor.canvas.height = h;
  $(".ui-wrapper").css({
    "width": w,
    "height": h
  });
  $("#vrtx-image-editor").css({
    "width": w,
    "height": h
  });
  editor.displayDimensions(w, h);
};

VrtxImageEditor.prototype.displayDimensions = function displayDimensions(w, h) {
  if ($("#vrtx-image-dimensions-crop").length) {
    $("#resource-width").val(w);
    $("#resource-height").val(h);
    // displayDebugInfo(this);
  } else {
    var dimensionHtml = '<div id="vrtx-image-dimensions-crop">'
                        + '<div class="vrtx-label-and-text">'
                          + '<div class="property-label">Bredde</div>'
                          + '<div class="vrtx-textfield" id="vrtx-textfield-width"><input id="resource-width" type="text" value="' + w + '" size="4" /></div>'
                        + '</div>'
                        + '<div class="vrtx-label-and-text">'
                          + '<div class="property-label">Høyde</div>'
                          + '<div class="vrtx-textfield" id="vrtx-textfield-height"><input id="resource-height" type="text" value="' + h + '" size="4" /></div>'
                        + '</div>';
    if(this.canvasSupported) {                      
      dimensionHtml += '<div id="vrtx-image-crop-button"><div class="vrtx-button">'
                     + '<input type="button" id="vrtx-image-crop" value="Start beskjæring..." /></div></div>'
                     + '<div id="vrtx-image-info" style="margin-top: 10px"></div>';
    }
    dimensionHtml  += '</div>';
    $(dimensionHtml).insertBefore("#vrtx-image-editor-preview");
  }
};

function displayDebugInfo(editor) {
  $("#vrtx-image-info").html("Width: " + editor.rw + " Height: " + editor.rh + " CropX: " + editor.cropX + " CropY: "
                           + editor.cropY + " CropWidth: " + editor.cropWidth + " CropHeight: " + editor.cropHeight
                           + " Scale: " + editor.scaleRatio + " ReverseScale: " + editor.reversedScaleRatio);

}

/*
 * Credits: http://hyankov.wordpress.com/2010/12/26/how-to-implement-html5-canvas-undo-function/
 * TODO: Undo/redo functionality. Use another canvas instead to avoid exporting to base64 before saving
 */
VrtxImageEditor.prototype.renderScaledImage = function renderScaledImage(insertImage) {
  var editor = this;
  
  var scaledImgSrc = editor.canvas.toDataURL("image/png");
  editor.scaledImg.src = scaledImgSrc;
  editor.scaledImg.onload = function(insertImage) {
    if(insertImage) {
      var tmpCanvas = $("#vrtx-image-editor-preview-image")[0];
      tmpCanvas.style.display = "block";
      var tmpCtx = tmpCanvas.getContext('2d');
      tmpCanvas.width = editor.rw;
      tmpCanvas.height = editor.rh;   
      if(editor.rw >= 230 && editor.rh >= 50) {
        $("#vrtx-image-editor-wrapper-loading-info").css({"width": editor.rw + "px", "height": editor.rh + "px"});
        $("#vrtx-image-editor-wrapper-loading-info-text").css({"height": "100%", "background": "#555", "opacity": "0.8"});
        $("#vrtx-image-editor-wrapper-loading-info-text span").css({"left": (Math.round((editor.rw - 220) / 2) + 5) + "px",
                                                                    "top": (Math.round((editor.rh - 40) / 2) + 5) + "px", "color": "#fff"});
      } else { // Just put it under..
        $("#vrtx-image-editor-wrapper-loading-info").css({"width": editor.rw + "px", "height": editor.rh + 50 + "px"});
        $("#vrtx-image-editor-wrapper-loading-info-text").css({"height": editor.rh + 50 + "px", "background": "transparent"});
        $("#vrtx-image-editor-wrapper-loading-info-text span").css({"left": "0px", "top": editor.rh + 30 + "px", "color": "#000"}); 
      }
      tmpCtx.drawImage(editor.scaledImg, 0, 0);
      $("#vrtx-image-editor-interpolation-complete").show(0);
    } else {
      editor.ctx.drawImage(editor.scaledImg, 0, 0);
    }
  };
};

String.prototype.endsWith = function(str) 
{return (this.match(str+"$")==str)}

VrtxImageEditor.prototype.scaleLanczos = function scaleLanczos(lobes) {
  var editor = this;

  editor.renderScaledImage(true);
  new thumbnailer(editor, lobes);
}

/* Thumbnailer / Lanczos algorithm for downscaling
 * Credits: http://stackoverflow.com/questions/2303690/resizing-an-image-in-an-html5-canvas
 *
 * Modified by USIT to use Web Workers if supported for process1 and process2 (otherwise degrade to setTimeout)
 *
 * TODO: Optimize and multiple Web Workers pr. process (tasking)
 *
 */

/* elem: Canvas element
 * ctx: Canvas 2D context 
 * img: Image element
 * sx: Scaled width
 * lobes: kernel radius (e.g. 3)
 */
function thumbnailer(editor, lobes) {
  var elem = editor.canvas;
  var ctx = editor.ctx;
  var img = editor.img;
  var sx = editor.rw;

  var canvas = elem;
  elem.width = img.width;
  elem.height = img.height;
  elem.style.display = "none";    
  $("#vrtx-image-editor-wrapper").addClass("loading");
  $("#vrtx-image-crop").attr("disabled", "disabled");
  ctx.drawImage(img, 0, 0);
  
  var w = sx;
  var h = editor.rh;
  var ratio = editor.reversedScaleRatio;
  var data = {
    src: ctx.getImageData(editor.cropX, editor.cropY, editor.cropWidth, editor.cropHeight),
    lobes: lobes,
    dest: {
      width: w,
      height: h,
      data: new Array(w * h * 3)
    },
    ratio: ratio,
    rcp_ratio: 2 / ratio,
    range2: Math.ceil(ratio * lobes / 2),
    cacheLanc: {},
    center: {},
    icenter: {}
  };
  
  $("#vrtx-image-editor-wrapper-loading-info").show(0);

  // Used for Web Workers or setTimeout (inject scripts and use methods inside)
  var process1Url = '/vrtx/__vrtx/static-resources/js/image-editor/lanczos-process1.js';
  var process2Url = '/vrtx/__vrtx/static-resources/js/image-editor/lanczos-process2.js';

  if (false) { // Use Web Worker if supported. TODO: fix problem hangs on some dimensions
    var workerLanczosProcess1 = new Worker(process1Url);
    var workerLanczosProcess2 = new Worker(process2Url); 
    workerLanczosProcess1.postMessage(data);
    workerLanczosProcess1.addEventListener('message', function(e) {
      var data = e.data;
      if(data) {   
        canvas.width = data.dest.width;
        canvas.height = data.dest.height;
        ctx.drawImage(img, 0, 0);
        data.src = ctx.getImageData(0, 0, data.dest.width, data.dest.height);
        workerLanczosProcess2.postMessage(data);
      } 
    }, false);
    workerLanczosProcess2.addEventListener('message', function(e) { 
       var data = e.data;
       if(data) { 
         ctx.putImageData(data.src, 0, 0);
         editor.renderScaledImage(false);   
         editor.save();
         elem.style.display = "block";
         $("#vrtx-image-editor-preview").removeClass("loading");
         $("#vrtx-image-crop").removeAttr("disabled"); 
       }
     }, false);
  } else { // Otherwise gracefully degrade to using setTimeout
    var headID = document.getElementsByTagName("head")[0];  
    var process1Script = document.createElement('script');
    var process2Script = document.createElement('script');
    process1Script.type = 'text/javascript';
    process2Script.type = 'text/javascript';
    process1Script.src = process1Url;
    process2Script.src = process2Url;
    headID.appendChild(process1Script);
    headID.appendChild(process2Script);

    process1Script.onload = function() {
      var u = 0; 
      var lanczos = lanczosCreate(data.lobes);
      var percent10 = Math.round(data.dest.width * 0.1);
      var percent10count = 10;
      var proc1 = setTimeout(function() {
        data = process1(data, u, lanczos);
        if(++u < data.dest.width) {
          setTimeout(arguments.callee, 0);
          if(u % percent10 == 0) {
            $("#vrtx-image-editor-interpolation-complete").val(percent10count);
            percent10count += 10;
          }
        } else {
          var proc2 = setTimeout(function() {
            canvas.width = data.dest.width;
            canvas.height = data.dest.height;
            ctx.drawImage(img, 0, 0);
            data.src = ctx.getImageData(0, 0, data.dest.width, data.dest.height);
            data = process2(data);
            ctx.putImageData(data.src, 0, 0);
            editor.renderScaledImage(false);  
            editor.save();
            elem.style.display = "block";
            $("#vrtx-image-editor-wrapper-loading-info").hide(0);
            $("#vrtx-image-editor-preview").removeClass("loading");
            $("#vrtx-image-crop").removeAttr("disabled"); 
          }, 0);
        }
      }, 0);
    }
  }
}

/*
 * Crop plugin
 * Credits: http://www.script-tutorials.com/demos/197/index.html
 * TODO: optimize
 * Modified slightly by USIT
 */

var iMouseX, iMouseY = 1;
var theSelection;

// Define Selection constructor
function Selection(x, y, w, h) {
  this.x = x; // initial positions
  this.y = y;
  this.w = w; // and size
  this.h = h;
  this.px = x; // extra variables to dragging calculations
  this.py = y;
  this.csize = 6; // resize cubes size
  this.csizeh = 10; // resize cubes size (on hover)
  this.bHow = [false, false, false, false]; // hover statuses
  this.iCSize = [this.csize, this.csize, this.csize, this.csize]; // resize cubes sizes
  this.bDrag = [false, false, false, false]; // drag statuses
  this.bDragAll = false; // drag whole selection
}

// Define Selection draw method
Selection.prototype.draw = function (editor) {
  editor.ctx.strokeStyle = '#000';
  editor.ctx.lineWidth = 2;
  editor.ctx.strokeRect(this.x, this.y, this.w, this.h);
  // draw part of original image
  if (this.w > 0 && this.h > 0) {
    editor.ctx.drawImage(editor.scaledImg, this.x, this.y, this.w, this.h, this.x, this.y, this.w, this.h);
  }
  // draw resize cubes
  editor.ctx.fillStyle = '#fff';
  editor.ctx.fillRect(this.x - this.iCSize[0], this.y - this.iCSize[0], this.iCSize[0] * 2, this.iCSize[0] * 2);
  editor.ctx.fillRect(this.x + this.w - this.iCSize[1], this.y - this.iCSize[1], this.iCSize[1] * 2, this.iCSize[1] * 2);
  editor.ctx.fillRect(this.x + this.w - this.iCSize[2], this.y + this.h - this.iCSize[2], this.iCSize[2] * 2, this.iCSize[2] * 2);
  editor.ctx.fillRect(this.x - this.iCSize[3], this.y + this.h - this.iCSize[3], this.iCSize[3] * 2, this.iCSize[3] * 2);
}

function drawScene(editor) { // Main drawScene function
  editor.ctx.clearRect(0, 0, editor.canvas.width, editor.canvas.height); // clear canvas
  // draw source image
  editor.ctx.drawImage(editor.scaledImg, 0, 0);
  // and make it darker
  editor.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  editor.ctx.fillRect(0, 0, editor.canvas.width, editor.canvas.height);
  // draw selection
  theSelection.draw(editor);
}

function initSelection(editor) {
  // create initial selection
  theSelection = new Selection(40, 40, editor.rw - 40, editor.rh - 40);
  $('#vrtx-image-editor').bind("mousemove", function (e) { // binding mouse move event
    var canvasOffset = $(editor.canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    // in case of drag of whole selector
    if (theSelection.bDragAll) {
      theSelection.x = iMouseX - theSelection.px;
      theSelection.y = iMouseY - theSelection.py;
    }
    for (i = 0; i < 4; i++) {
      theSelection.bHow[i] = false;
      theSelection.iCSize[i] = theSelection.csize;
    }
    // hovering over resize cubes
    if (iMouseX > theSelection.x - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[0] = true;
      theSelection.iCSize[0] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh 
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.csizeh) {
      theSelection.bHow[1] = true;
      theSelection.iCSize[1] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[2] = true;
      theSelection.iCSize[2] = theSelection.csizeh;
    }
    if (iMouseX > theSelection.x - theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.h - theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h + theSelection.csizeh) {
      theSelection.bHow[3] = true;
      theSelection.iCSize[3] = theSelection.csizeh;
    }
    // in case of dragging of resize cubes
    var iFW, iFH;
    if (theSelection.bDrag[0]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = iMouseY - theSelection.py;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[1]) {
      var iFX = theSelection.x;
      var iFY = iMouseY - theSelection.py;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = theSelection.h + theSelection.y - iFY;
    }
    if (theSelection.bDrag[2]) {
      var iFX = theSelection.x;
      var iFY = theSelection.y;
      iFW = iMouseX - theSelection.px - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (theSelection.bDrag[3]) {
      var iFX = iMouseX - theSelection.px;
      var iFY = theSelection.y;
      iFW = theSelection.w + theSelection.x - iFX;
      iFH = iMouseY - theSelection.py - iFY;
    }
    if (iFW > theSelection.csizeh * 2 && iFH > theSelection.csizeh * 2) {
      theSelection.w = iFW;
      theSelection.h = iFH;
      theSelection.x = iFX;
      theSelection.y = iFY;
    }
    drawScene(editor);
  });
  $('#vrtx-image-editor').bind("mousedown", function (e) { // binding mousedown event
    var canvasOffset = $(editor.canvas).offset();
    iMouseX = Math.floor(e.pageX - canvasOffset.left);
    iMouseY = Math.floor(e.pageY - canvasOffset.top);
    theSelection.px = iMouseX - theSelection.x;
    theSelection.py = iMouseY - theSelection.y;
    if (theSelection.bHow[0]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[1]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y;
    }
    if (theSelection.bHow[2]) {
      theSelection.px = iMouseX - theSelection.x - theSelection.w;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (theSelection.bHow[3]) {
      theSelection.px = iMouseX - theSelection.x;
      theSelection.py = iMouseY - theSelection.y - theSelection.h;
    }
    if (iMouseX > theSelection.x + theSelection.csizeh
     && iMouseX < theSelection.x + theSelection.w - theSelection.csizeh
     && iMouseY > theSelection.y + theSelection.csizeh
     && iMouseY < theSelection.y + theSelection.h - theSelection.csizeh) {
      theSelection.bDragAll = true;
    }
    for (i = 0; i < 4; i++) {
      if (theSelection.bHow[i]) {
        theSelection.bDrag[i] = true;
      }
    }
  });
  $('#vrtx-image-editor').bind("mouseup", function (e) { // binding mouseup event
    theSelection.bDragAll = false;
    for (i = 0; i < 4; i++) {
      theSelection.bDrag[i] = false;
    }
    theSelection.px = 0;
    theSelection.py = 0;
  });
  drawScene(editor);
}

/* ^ Vortex HTML5 Canvas image editor */
