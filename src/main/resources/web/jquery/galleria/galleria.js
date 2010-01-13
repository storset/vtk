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
      
      if(width > 730 && width > height) {
    	  $(".galleria_wrapper img").css('width', '730px');   
      } else if (height > 438 && height > width) {
    	  $(".galleria_wrapper img").css('width', '440px');
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