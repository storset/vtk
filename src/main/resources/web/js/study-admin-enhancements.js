function hideShowStudy(typeToDisplay) {
  var container = $("#editor");
  // TODO: possible use container.attr("class", "").addClass(""); instead
  switch (typeToDisplay) {
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
  try {
    var typeToDisplay = $('#typeToDisplay').val();
    hideShowStudy(typeToDisplay);
  }
  catch (err) {
    return false;
  }
  if($('#typeToDisplay').length) { // Check that it is the correct document
    for(var grouped = $(".vrtx-grouped"), i = grouped.length; i--;) { // Because accordion needs one content wrapper
      $(grouped[i]).find("> *:not(.header)").wrapAll("<div />");
    }
    $("#editor").accordion({ header: "> div > .header", autoHeight: false }); // should have "active: false" and "collapsible: true" but it seems to fail with error msg
  }
  $(document).on('change', '#typeToDisplay', function () {
    hideShowStudy($(this).val());
  });
});
