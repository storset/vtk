jQuery( function($) {
  $('.vrtx-gallery').addClass('default_galleria_style');
  $('.nav').css('display', 'none');
  $('.nav a:first').addClass('previous'); //because of missing id/class on links in nav
  $('ul.default_galleria_style').galleria( {
    history :false,
    clickNext :true,
    insert :undefined,
    onImage : function() {
      $('.nav').css('display', 'block');

      var width = $(".galleria_wrapper img").width();
      var height = $(".galleria_wrapper img").height();
      
      if (height > 533) { //Landscape 4:3 aspect ratio to 710px
    	  $(".galleria_wrapper img").css('height', '533px');
      }
 
    }
  });
});

//View on black / white background

function toWhiteBG() {
  $('body .vrtx-image-gallery').css( { 'color':'#555', 'background-color': '#fff' } );
  $('#vrtx-display-on-white').css('color', '#555'); 
  $('#vrtx-display-on-black').css('color', '#334488');
}
function toBlackBG() {
  $('body .vrtx-image-gallery').css( { 'color': '#eee', 'background-color': '#000' } );
  $('#vrtx-display-on-black').css('color', '#eee'); 
  $('#vrtx-display-on-white').css( 'color', '#334488');
}