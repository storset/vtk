/*
 *  Vortex Admin - Embedded listing
 *
 *  Async actions a la regular admin (but some different "wirering")
 *
 */
 
vrtxAdmin._$(document).ready(function () {
  var vrtxAdm = vrtxAdmin;

  /* Delete action */
  vrtxAdm.getFormAsync({
    selector: ".delete-action",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "#directory-listing",
    nodeType: "div",
    simultanSliding: true,
    funcAfterComplete: updateIframeHeight
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    post: true,
    funcComplete: updateListing
  });
  
  /* Upload action */
  if (vrtxAdm.isIOS5) {
    $("#upload-action").hide();
  } else {
    vrtxAdm.getFormAsync({
      selector: "#upload-action",
      selectorClass: "vrtx-admin-form",
      insertAfterOrReplaceClass: "#directory-listing",
      nodeType: "div",
      focusElement: "",
      simultanSliding: true,
      funcBeforeComplete: function() {
        $("#upload-action").hide();
      },
      funcComplete: function (p) {
        vrtxAdm.initFileUpload();
      },
      funcAfterComplete: updateIframeHeight
    });
    vrtxAdm.completeFormAsync({
      selector: "form#fileUploadService-form .vrtx-focus-button",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: function(opts) {
        updateIframeHeight(250);
        return ajaxUpload(opts);
      },
      funcComplete: function() {
        $("#upload-action").show();
        updateListing();
      }
    });
    // Only if received upload parameter
    if(gup("upload", location.href) === "true") {
      $("#upload-action").hide().click();
    }
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
    }
  }); 
}

function updateIframeHeight(minH) {
  if (window != top) {
    var minHeight = (typeof minH === "number") ? minH : 0; /* Use a minimum height if specified in function parameter */
    var iframes = $(window.parent.document).find(".admin-fixed-resources-iframe").filter(":visible");
    for (var i = 0, len = iframes.length; i < len; i++) {
      var iframe = iframes[i];
      /* Taken from iframe-view.js */
      var computedHeight = Math.max(minHeight, Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 15);
      computedHeight = (computedHeight - ($.browser.msie ? 4 : 0));
      iframe.style.height = computedHeight + 'px';
    }
  }
}