/*********************************************************************************************/
/* Script to dynamically alter IFRAME height, making Kupu editor textarea fit browser window */
/*********************************************************************************************/


// Initialize resize script:
resizeEditorIframe()


function resizeEditorIframe() {
    // Gecko-based browsers (Firefox, Mozilla, Netscape, Camino etc.)
    if( window.addEventListener ) {
        window.addEventListener( "load", dyniframesize, false );
        window.addEventListener( "resize", dyniframesize, false );
    }
    // Internet Explorer
    else if( window.attachEvent ) {
        window.attachEvent( "onload", dyniframesize );
        window.attachEvent( "onresize", dyniframesize );
    }
    else {
        window.onLoad = dyniframsize;
        window.onResize = dyniframesize;
    }
} // end function


// default variables for resize function
var iframeid = "kupu-editor";

// value in 'px'
var geckoOffset = 320;
var iexplore56offset = 330;
var iexplore7offset = 350;  // window is slightly smaller due to tabs


function dyniframesize() {
    if( !is_safari ) {
        if( document.getElementById ) { //begin resizing iframe procedure
            var editIframe = document.getElementById(iframeid);
                        
            if( editIframe && !window.opera ) {
                try {
                    // Gecko-based browsers (Firefox, Mozilla, Netscape, Camino etc.)
                    if( editIframe.contentDocument && window.innerHeight ) { 
                        var height = window.innerHeight - geckoOffset;
                        editIframe.style.height = height.toString() + "px";
                    }
                    // Internet Explorer
                    else if( editIframe.Document && editIframe.Document.body.clientHeight ) {
                    	// Internet Explorer 7
                    	if (typeof document.documentElement.style.maxHeight != 'undefined')  // only implemented in IE7+
                    		var offset = iexplore7offset;
                        // Internet Explorer 5 and 6
                        else
                        	var offset = iexplore56offset;
                        window.status = document.documentElement.clientHeight;
                        var height = parseInt(document.documentElement.clientHeight) - offset;
                        editIframe.style.height = height.toString() + "px";
                        // Mulig "document.body.clientHeight" skal brukes for IE4/IE5...?
                    }
                } catch (exception) {
                    alert('Exception: ' + exception.message)
                }
            }
        }
                
        // kupu iframe handling for non-supported browsers is done by the Vortikal core system
        // (browser-sniffer script etc)
    
    }
} //end function
