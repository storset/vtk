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
    	  $(".galleria_wrapper").css('height', '548px');

          var browser = navigator.appName;
          var version = navigator.appVersion;
          if(browser == "Microsoft Internet Explorer" && version == 7) {
            //Better downscaling in IE7 (explicit set bicubic interpolation)
            $(".galleria_wrapper img").css('-ms-interpolation-mode', 'bicubic');
          }          
      } else if (height < 140) {
    	  $(".galleria_wrapper").css('height', '140px');
      } else {
    	  $(".galleria_wrapper").css('height', height + 'px');
      }
      
      //Calculate where to place navigation paging links
      var top = (parseInt($(".galleria_wrapper").css('height')) / 2) - 17;
      
      var width = $(".galleria_wrapper img").width();
     
      if(width < 140) {
        width = 140;  
      } else if (width > 730) {
    	width = 730;  
      }

      $(".galleria_container").css('width', width + "px");
         
      //Center paging when image width is less than 730px
      if(width < 730) {
    	  prev = ((730 - width) / 2) - 35;
    	  width = width + ((730 - width) / 2);
      } else {
    	  prev = -35;  
      }
      
      $("a#vrtx-image-gallery-previous").css('top', top + "px");
      $("a#vrtx-image-gallery-previous").css('left', prev + "px");
      $("a#vrtx-image-gallery-next").css('top', top + "px");
      $("a#vrtx-image-gallery-next").css('left', width + "px");
 
    }
  });
});
