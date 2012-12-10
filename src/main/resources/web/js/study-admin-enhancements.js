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

  // 'How to search'-document
  var typeToDisplay = $("#typeToDisplay"); 
  if(typeToDisplay.length) { 
    hideShowStudy(typeToDisplay);
    $(document).on("change", "#typeToDisplay", function () {
      hideShowStudy($(this));
      $(".ui-accordion > .vrtx-string.last").removeClass("last");
      $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
    });    
    
     // Because accordion needs one content wrapper
    for(var grouped = $(".vrtx-grouped"), i = grouped.length; i--;) { 
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    $("#editor .properties").accordion({ header: "> div > .header",
                             autoHeight: false,
                             collapsible: true,
                             active: false
                           });
    $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
  }
  
  // Course description - hide/show semesters
  for(var semesters = ["teaching", "exam"], i = semesters.length, semesterId, semesterType; i--;) {
    semesterId = "#" + semesters[i] + "semester";
    semesterType = $(semesterId);
    if(semesterType.length) {
      hideShowSemester(semesterType);
      $(document).on("change", semesterId, function () {
        hideShowSemester($(this));
      });
    }
  }
  
  // 'Samlet program'-document
  var samletElm = $(".samlet-element");
  if(samletElm.length) {
    replaceTag(samletElm, "h6", "strong");
    replaceTag(samletElm, "h5", "h6");  
    replaceTag(samletElm, "h4", "h5");
    replaceTag(samletElm, "h3", "h4");
    replaceTag(samletElm, "h2", "h3");
    replaceTag(samletElm, "h1", "h2");
  }
});

function hideShowStudy(typeToDisplayElem) {
  var container = $("#editor");
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

function hideShowSemester(typeSemesterElem) {
  var container = $("#editor");
  var prefix = typeSemesterElem.attr("id") + "-selected";
  switch (typeSemesterElem.val()) {
    case "particular-semester":
      container.removeClass(prefix + "-every-other").addClass(prefix + "-particular");
      break;
    case "every-other":
      container.removeClass(prefix + "-particular").addClass(prefix + "-every-other");
      break;  
    default:
      container.removeClass(prefix + "-particular").removeClass(prefix + "-every-other");
      break;
  }
}

function replaceTag(selector, tag, replaceTag) {
  selector.find(tag).replaceWith(function() {
    return "<" + replaceTag + ">" + $(this).text() + "</" + replaceTag + ">";
  });
}

/* ^ Vortex Study admin enhancements */
