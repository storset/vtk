function hideShowStudy(typeToDisplay) {
  var container = $("#editor");
  switch (typeToDisplay) { // TODO: possible use container.attr("class", "").addClass(""); instead
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

$(document).ready(function () {
  var typeToDisplay = $('#typeToDisplay');
  if(typeToDisplay.length) { // Check that it is the correct document
    try {
      hideShowStudy(typeToDisplay.val());
    }
    catch (err) {
      vrtxAdmin.error({msg: err});
    }
    for(var grouped = $(".vrtx-grouped"), i = grouped.length; i--;) { // Because accordion needs one content wrapper
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    $("#editor").accordion({ header: "> div > .header",
                             autoHeight: false,
                             collapsible: true,
                             active: false
                           });
    $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
  }
  $(document).on('change', '#typeToDisplay', function () {
    hideShowStudy($(this).val());
    $(".ui-accordion > .vrtx-string").removeClass("last");
    $(".ui-accordion > .vrtx-string:visible:last").addClass("last");
  });
});
