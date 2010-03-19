// Vortex Simple Gallery jQuery plugin v0.1b
// w/ paging, centered thumbnail navigation and fade effect
// by Øyvind Hatland - UiO / USIT

(function ($) {
  $.fn.vrtxSGallery = function (wrapper, container, options, navFade) {
	  
	  //cache images
	  var images = new Array();
	  
	  //default animation settings
	  settings = jQuery.extend({ fadeInOutTime : 250, fadedOutOpacity: 0 }, options);

	  //Unobtrusive JavaScript
	  $(container + "-pure-css").addClass(container.substring(1));
	  $(container + "-nav-pure-css").addClass(container.substring(1) + "-nav");
		  
	  //paging (relative to li a.active)
	  addPagingClickAndHoverEvents("next", wrapper);
	  addPagingClickAndHoverEvents("prev", wrapper);  

	  initFirstImage();
		  
	  return this.each(function (i) {
		   
		   var link = generateLinkImage($("img", this), $(this), container);
	      
	       //cache images
	       images[i] = link;
	       
		   $(this).hover(
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo("250", 1);
		    	}
		    }, //on hover out
		    function () {
		    	if(!$(this).hasClass("active")) {
				 $("img", this).stop().fadeTo("250", 0.6);
		    	}
	       });
		   
		   $(this).click(function(e) {
		  	  //replace link and image (w/ fade effect down to fadedOutOpacity) + stop() current animation.
		      if(settings.fadeInOutTime > 0) {
			      $(wrapper + " " + container).stop().fadeTo(settings.fadeInOutTime, settings.fadedOutOpacity, function() {
			    	  //done fade out -> remove
			    	  $("a" + container + "-link", this).remove();
			    	  //append image
			    	  $(this).append(images[i]); 
			    	  //fade in and make sure calculations are done after fully loaded
			    	  $(this).fadeTo(settings.fadeInOutTime, 1, function() {
			    		  calculatePagingNavigationHeight();
			    	  });
			      });
		      } else {
		    	  $("a" + container + "-link", wrapper + " " + container).remove();
		    	  $(wrapper + " " + container).append(images[i]);
		    	  calculatePagingNavigationHeight();
		      }
		      //remove active classes
		      jQuery(wrapper + " ul li a").each(function(j) {
		    	if(jQuery(this).hasClass("active")) {
		    	   jQuery(this).removeClass("active");
		    	   jQuery("img", this).stop().fadeTo("250", 0.6);
		    	} else {
		    	   jQuery("img", this).stop().fadeTo("0", 0.6);
		    	}
		      });
		      
		      //add new active class
		  	  $(this).addClass("active");
		  	  //make sure opacity is 1
		  	  $("img", this).stop().fadeTo("0", 1);
	
		  	  addDescription($("img", this));

		      //prevent default event action
			  e.preventDefault(); 
			  
	      });
		  centerThumbnailImage($("img", this)); 
	 });
	  
	 function centerThumbnailImage(thumb) {
		 setTimeout(function() {
		 //center thumbnails images ..
		 //.. first horizontal
		 var imgWidth = $(thumb).width();
		 if(imgWidth > $(thumb).parent().width()) {
		    var leftAdjust = -(imgWidth - $(thumb).parent().width()) / 2;
			$(thumb).css("marginLeft", leftAdjust + "px");
		 }
		 //.. then vertical
		 var imgHeight = $(thumb).height();
		 if(imgHeight > $(thumb).parent().height()) {
			var topAdjust = -(imgHeight - $(thumb).parent().height()) / 2;
			$(thumb).css("marginTop", leftAdjust + "px"); 
		 }
		 }, 100);
	 }
	  
	 function addPagingClickAndHoverEvents(navClass, wrapper) {
	    $(wrapper + " " + " a." + navClass).click(function(h) {
	      if(navClass == "next") {
			  if($(wrapper + " ul li a.active").parent().next().length != 0) {
			    $(wrapper + " ul li a.active").parent().next().find("a").click();
			  } else {
				$(wrapper + " ul li:first a").click();
			  }
	      } else {
	    	  if($(wrapper + " ul li a.active").parent().prev().length != 0) {
	    		$(wrapper + " ul li a.active").parent().prev().find("a").click();
	          } else {
	    		$(wrapper + " ul li:last a").click();   
	          }
	      }
		  h.preventDefault(); 
		});
	    if(navFade) {
		    //Fading of transparent block and prev / next icon
		    $(wrapper + " " + " a." + navClass).stop().fadeTo("0", 0);
		    $(wrapper + " " + " a." + navClass + " span").stop().fadeTo("0", 0);
		    
		    $(wrapper + " " + " a." + navClass).hover(function () {
			  $(this).stop().fadeTo("250", 1);
			  $("span", this).stop().fadeTo("250", 0.2);
			}, function () { //hover out
			  $(this).stop().fadeTo("250", 0);
			  $("span", this).stop().fadeTo("250", 0);
			});
	    }
	 }
	  
     function initFirstImage() {
		
	   var link = generateLinkImage(wrapper + " ul li a.active img", wrapper + " ul li a.active", container);
      
       $("a" + container + "-link", wrapper + " " + container).remove();
       $(wrapper + " " + container).append(link);
      
       addDescription(wrapper + " ul li a.active img");
       calculatePagingNavigationHeight()
	  
	   //set all thumbnails not active to 0.6 opacity
	   jQuery(wrapper + " ul li a").each(function(j) {
		 if(jQuery(this).hasClass("active")) {
		 } else {
		   jQuery("img", this).stop().fadeTo("0", 0.6);
		 }
	   });
	 }
     
     function generateLinkImage(theimage, thelink) {
       //create image
  	   var img = new Image();
  	   var src = $(theimage).attr("src").split("?")[0]; 
  	   var alt = $(theimage).attr("alt");
  	   img.src = src; img.alt = alt;
  	  
  	   //create link
  	   link = document.createElement("a"); 
  	   link.setAttribute("href", $(thelink).attr("href"));
  	   link.setAttribute("class", container.substring(1) + "-link");
  	   // IE
  	   link.setAttribute("className", container.substring(1) + "-link");
  	      
  	   //append img inside link
  	   $(link).append(img);

  	   return link;
  	 }
	  
	 function addDescription(fromSource) {
	   $(wrapper + " " + container + "-description").remove();
	   $("<div class='" + container.substring(1) + "-description'>" 
	          + $(fromSource).attr("alt") + "</div>").insertAfter(wrapper + " " + container);
	 }

	 function calculatePagingNavigationHeight() {
	   var imgHeight = $(wrapper + " " + container + " img").height();
	   $(wrapper + " " + container + "-nav a").css("height", imgHeight);
	   $(wrapper + " " + container + "-nav span").css("height", imgHeight);
	 }
  };
})(jQuery);