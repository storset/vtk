
var urlobj;

function browseServer(obj, editorBase, baseFolder, editorBrowseUrl, type) {
    urlobj = obj;
    if (type) {
        openServerBrowser(editorBase + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + 
                '&Type=' + type + '&Connector=' + editorBrowseUrl,
                screen.width * 0.7,
                screen.height * 0.7 ) ;

    } else {
        openServerBrowser(editorBase + '/plugins/filemanager/browser/default/browser.html?BaseFolder=' + baseFolder + 
                '&Type=Image&Connector=' + editorBrowseUrl,
                screen.width * 0.7,
                screen.height * 0.7 ) ;
    }
}

function openServerBrowser(url, width, height) {
    var iLeft = (screen.width  - width) / 2 ;
    var iTop  = (screen.height - height) / 2 ;
    var sOptions = "toolbar=no,status=no,resizable=yes,dependent=yes" ;
    sOptions += ",width=" + width ;
    sOptions += ",height=" + height ;
    sOptions += ",left=" + iLeft ;
    sOptions += ",top=" + iTop ;
    var oWindow = window.open(url, "BrowseWindow", sOptions) ;
}

//Callback from the FCKEditor image browser:
function SetUrl(url, width, height, alt) {
    url = decodeURIComponent(url);
    if(urlobj) {
    	document.getElementById(urlobj).value = url ;
    }    
    oWindow = null;
    previewImage(urlobj);
}