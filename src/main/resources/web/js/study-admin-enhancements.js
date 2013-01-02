/*
 *  Vortex Study admin enhancements
 *
 *  TODO: delete file and move content to editor-ck-setup-helper.js (VTK-3047)
 *        also other editor specific code in admin-enhancements.js
 *
 */

$(document).ready(function () {

  // When ui-helper-hidden class is added => we need to add 'first'-class to next element (if it is not last and first of these)
  $(".ui-helper-hidden").filter(":not(:last)").filter(":first").next().addClass("first");
  // TODO: make sure these are NOT first so that we can use pure CSS
  
  var editor = $("#editor");
  var docType = editor[0].className;

  if(docType === "vrtx-hvordan-soke") {
    hideShowStudy(editor, $("#typeToDisplay"));
    $(document).on("change", "#typeToDisplay", function () {
      hideShowStudy(editor, $(this));
      editor.find(".ui-accordion > .vrtx-string.last").removeClass("last");
      editor.find(".ui-accordion > .vrtx-string:visible:last").addClass("last");
    });    
     
    // Because accordion needs one content wrapper
    for(var grouped = editor.find(".vrtx-grouped"), i = grouped.length; i--;) { 
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    editor.find(".properties").accordion({ header: "> div > .header",
                               autoHeight: false,
                               collapsible: true,
                               active: false
                             });
    editor.find(".ui-accordion > .vrtx-string:visible:last").addClass("last");
    
  } else if(docType === "vrtx-course-description") { 
    for(var semesters = ["teaching", "exam"], i = semesters.length, semesterId, semesterType; i--;) {
      semesterId = "#" + semesters[i] + "semester";
      semesterType = $(semesterId);
      if(semesterType.length) {
        hideShowSemester(editor, semesterType);
        $(document).on("change", semesterId, function () {
          hideShowSemester(editor, $(this));
        });
      }
    }
    setShowHide('course-fee', ["course-fee-amount"], false);
  } else if(docType === "vrtx-semester-page") {
    for(var grouped = editor.find(".vrtx-grouped[class*=link-box]"), i = grouped.length; i--;) { 
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    grouped.wrapAll("<div id='link-boxes' />");
    editor.find("#link-boxes").accordion({ header: "> div > .header",
                                     autoHeight: false,
                                     collapsible: true,
                                     active: false
                                   });
  } else if(docType === "vrtx-samlet-program") {
    var samletElm = editor.find(".samlet-element");
    replaceTag(samletElm, "h6", "strong");
    replaceTag(samletElm, "h5", "h6");  
    replaceTag(samletElm, "h4", "h5");
    replaceTag(samletElm, "h3", "h4");
    replaceTag(samletElm, "h2", "h3");
    replaceTag(samletElm, "h1", "h2");
  }
});

function hideShowStudy(container, typeToDisplayElem) {
  switch (typeToDisplayElem.val()) {
    case "so":
      container.removeClass("nm").removeClass("em").addClass("so");
      break;
    case "nm":
      container.removeClass("so").removeClass("em").addClass("nm");
      break;
    case "em":
      container.removeClass("so").removeClass("nm").addClass("em");
      break;
    default:
      container.removeClass("so").removeClass("nm").removeClass("em");
      break;
  }
}

function hideShowSemester(container, typeSemesterElem) {
  var prefix = typeSemesterElem.attr("id") + "-selected";
  switch (typeSemesterElem.val()) {
    case "particular-semester":
      container.removeClass(prefix + "-every-other").removeClass(prefix + "-other").addClass(prefix + "-particular");
      break;
    case "every-other":
      container.removeClass(prefix + "-particular").removeClass(prefix + "-other").addClass(prefix + "-every-other");
      break;
    case "other":
      container.removeClass(prefix + "-every-other").removeClass(prefix + "-particular").addClass(prefix + "-other");
      break;  
    default:
      container.removeClass(prefix + "-particular").removeClass(prefix + "-every-other").removeClass(prefix + "-other");
      break;
  }
}

function replaceTag(selector, tag, replaceTag) {
  selector.find(tag).replaceWith(function() {
    return "<" + replaceTag + ">" + $(this).text() + "</" + replaceTag + ">";
  });
}

/* ^ Vortex Study admin enhancements */
