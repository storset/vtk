/*
 *  Vortex Admin - Embedded listing
 *
 *  With async actions a la regular admin
 *
 */

vrtxAdmin._$(document).ready(function () {
  var vrtxAdm = vrtxAdmin;
  
  /* Iframe busting */
  $(document).on("click", "td.name a", function(e) {
    if (window != top) {
      window.parent.location.href = this.href;
      return false;
    }
  });
  
  /* Delete action */
  vrtxAdm.getFormAsync({
    selector: ".delete-action",
    selectorClass: "globalmenu",
    insertAfterOrReplaceClass: "tr",
    isReplacing: true,
    findClosest: true,
    nodeType: "tr",
    simultanSliding: true,
    funcAfterComplete: updateIframeHeight
  });
  vrtxAdm.completeFormAsync({
    selector: "form#deleteResourceService-form input[type=submit]",
    isReplacing: true,
    post: true,
    funcAfterComplete: updateListing
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
      selector: "form#fileUploadService-form input[type=submit]",
      errorContainer: "errorContainer",
      errorContainerInsertAfter: "h3",
      post: true,
      funcProceedCondition: function(opts) {
        updateIframeHeight(250);
        return ajaxUpload(opts);
      },
      funcAfterComplete: function() {
        $("#upload-action").show();
        updateListing();
      }
    });
    // Only if received upload parameter
    if(gup("upload", location.href) === "true") {
      $("#upload-action").click();
    } else {
      $(window).load(updateIframeHeight);
    }
  }
});

function updateListing() {
  vrtxAdmin.serverFacade.getHtml(location.href, {
    success: function (results, status, resp) {
      var html = $($.parseHTML(results)).filter("#directory-listing").html();
      vrtxAdmin.cachedBody.find("#directory-listing").html(html);
      updateIframeHeight();
    }
  }); 
}

function updateIframeHeight(minH) {
  if (window != top) {
    var minHeight = (typeof minH === "number") ? minH : 0;
    var parent = $(window.parent.document);
    if(!parent.find("html").hasClass("embedded")) {
      var iframes = parent.find(".session .accordion-content").filter(":visible").find("iframe");
    } else {
      var iframes = parent.find("iframe");
    }
    for (var i = 0, len = iframes.length; i < len; i++) {
      var iframe = iframes[i];
      if(window === iframe.contentWindow) {
        var iframeElm = $(iframe);
        if(iframeElm.filter(":visible").length) {
          try {
            var computedHeight = Math.max(minHeight, Math.ceil(iframe.contentWindow.document.body.offsetHeight) + 15);
            computedHeight = (computedHeight - ($.browser.msie ? 4 : 0));
            iframe.style.height = computedHeight + 'px';
          } catch(ex) {}
        } else {
          var timerUpdateIframeHeight = setTimeout(arguments.callee, 150);
        }
        break;
      }
    }
  }
}