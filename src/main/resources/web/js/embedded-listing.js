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
    simultanSliding: true
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    post: true,
    funcComplete: updateListing
  });
  
  /* Upload action */
  $("#upload-action").hide();
  if (vrtxAdm.isIOS5) {
  } else {
    vrtxAdm.getFormAsync({
      selector: "#upload-action",
      selectorClass: "vrtx-admin-form",
      insertAfterOrReplaceClass: "#upload-action-container span",
      nodeType: "div",
      focusElement: "",
      funcComplete: function (p) {
        vrtxAdm.initFileUpload();
      },
      simultanSliding: true
    });
    $(document).on("click", ".vrtx-file-upload", function () {
      $("#file").click();
    });
    
    // Auto-trigger Upload when have choosen files
    $(document).on("change", "#file", function () {
      $("form#fileUploadService-form .vrtx-focus-button").click();
    });
    vrtxAdm.completeFormAsync({
      selector: "form#fileUploadService-form .vrtx-focus-button",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: ajaxUpload,
      funcComplete: updateListing
    });
    $("#upload-action").click();
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
      $("#upload-action").click();
    }
  }); 
}