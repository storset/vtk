function hideShowStudy(typeToDisplay) {
  var container = $("#editor");
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
  $(document).on('change', '#typeToDisplay', function () {
    hideShowStudy($(this).val());
  });
});