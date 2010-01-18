jQuery( function($) {
  $('.vrtx-gallery').addClass('default_galleria_style');
  $('.nav').css('display', 'none');
  $('.default_galleria_style li').each(function (i) {
    if((i+1) % 5 == 0) {
      $(this).css('marginRight', '0');	
    }
  });

  $('ul.default_galleria_style').galleria( {
    history :false,
    clickNext :true,
    insert :undefined,
    onImage : function() {
      $('.nav').css('display', 'block');

      var height = $(".galleria_wrapper img").height();
      
      if (height > 548) { //Landscape 4:3 aspect ratio to 730px
    	  $(".galleria_wrapper img").css('height', '548px');
      }
 
    }
  });
});