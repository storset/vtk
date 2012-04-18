function hideShowStudy(typeToDisplay) {
  var container = $("#editor");
  switch (typeToDisplay) {
    case "so":
      container.removeClass("nm, em").addClass("so");
      break;
    case "nm":
      container.removeClass("so, em").addClass("nm");
      break;
    case "em":
      container.removeClass("so, nm").addClass("em");
      break;
    default:
      container.removeClass("so, nm, em");
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
  $(document).on('change', '#typeToDisplay', function () {
    hideShowStudy($(this).val());
  });
});