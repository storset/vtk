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

//View on black / white
//TODO: Localize text

function toWhiteBG() {
  $('body').css( { 'color':'#555', 'background-color': '#fff' } );
  $('#vrtx-display-on-white').css('color', '#555'); 
  $('#vrtx-display-on-black').css('color', '#334488');
}
function toBlackBG() {
  $('body').css( { 'color': '#eee', 'background-color': '#000' } );
  $('#vrtx-display-on-black').css('color', '#eee'); 
  $('#vrtx-display-on-white').css( 'color', '#334488');
}

$(document).ready(function() {
   var htmlBGChange = "<div id='vrtx-image-gallery-colors'>View on: <a id='vrtx-display-on-white' href='#' onClick='toWhiteBG();'>White</a>";
   htmlBGChange += " | <a id='vrtx-display-on-black' href='#' onClick='toBlackBG();'>Black</a>";
   htmlBGChange += "</div>";
   $(htmlBGChange).insertAfter("ul.vrtx-gallery");
});