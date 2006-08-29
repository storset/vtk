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

var offset = 300; // value in 'px'
//var geckoOffset = 300;
//var iexploreOffset = 300;
// testing viste at det er greit med samme offset for begge browser-typer


function dyniframesize() {
    
    if( !is_safari ) {
        if( document.getElementById ) { //begin resizing iframe procedure
            var editIframe = document.getElementById(iframeid);
            
            if( editIframe && !window.opera ) {
                try {
                    // Gecko-based browsers (Firefox, Mozilla, Netscape, Camino etc.)
                    if( editIframe.contentDocument && window.innerHeight ) { 
                        var height = window.innerHeight - offset; //- geckoOffset;
                        editIframe.style.height = height.toString() + "px";
                    }
                    // Internet Explorer 5 and later
                    else if( editIframe.Document && editIframe.Document.body.clientHeight ) {
                        // Mulig "document.body.clientHeight" skal brukes for IE4/IE5...?
                        window.status = document.documentElement.clientHeight;
                        var height = parseInt(document.documentElement.clientHeight) - offset; //- iexploreOffset;
                        editIframe.style.height = height.toString() + "px";
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
