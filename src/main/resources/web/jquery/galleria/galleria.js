jQuery( function($) {
  $('.vrtx-gallery').addClass('default_galleria_style');
  $('.nav').css('display', 'none');
  $('ul.default_galleria_style').galleria( {
    history :false,
    clickNext :false,
    insert :undefined,
    onImage : function() {
      $('.nav').css('display', 'block');
    }
  });
});
