function hideShowStudy(typeToDisplayElem) {
  var container = $("#editor");
  switch (typeToDisplayElem.val()) { // TODO: possible use container.attr("class", "").addClass(""); instead
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
  var prefix = typeSemesterElem.attr("id") + "-valgt";
  switch (typeSemesterElem.val()) { // TODO: possible use container.attr("class", "").addClass(""); instead
    case "bestemt-semester":
      container.removeClass(prefix + "-annet").addClass(prefix + "-bestemt-semester");
      break;
    case "annet":
      container.removeClass(prefix + "-bestemt-semester").addClass(prefix + "-annet");
      break;
    default:
      container.removeClass(prefix + "-annet").removeClass(prefix + "-bestemt-semester");
      break;
  }
}

function replaceTag(selector, tag, replaceTag) {
  selector.find(tag).replaceWith(function() {
    return "<" + replaceTag + ">" + $(this).text() + "</" + replaceTag + ">";
  });
}

$(document).ready(function () {

  // 'How to search'-document
  var typeToDisplay = $("#typeToDisplay"); 
  if(typeToDisplay.length) { 
    hideShowStudy(typeToDisplay);
    for(var grouped = $(".vrtx-grouped"), i = grouped.length; i--;) { // Because accordion needs one content wrapper
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    $("#editor").accordion({ header: "> div > .header",
                             autoHeight: false,
                             collapsible: true,
                             active: false
                           });
    $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
    
    $(document).on("change", "#typeToDisplay", function () {
      hideShowStudy($(this).val());
      $(".ui-accordion > .vrtx-string.last").removeClass("last");
      $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
    });
  }
  
  // Course description - hide/show semesters
  for(var semesters = ["undervisning", "eksamen"], i = semesters.length, semesterId, semesterType; i--;) {
    semesterId = "#" + semesters[i] + "ssemester";
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
