function xmlClean(randomdata, whitelist_elements, pdiv){
	var doc;
	var xhtml = randomdata;

	//document.getElementById("iframeID").contentWindow.document.body.innerHTML;
	/* Parse XHTML */{
		pdiv.innerHTML = "<div>" + xhtml + "</div>";
		doc = pdiv;
		contentDom = doc;
	}

	var cleanxml;

	if(xhtml.indexOf("<body")!=-1 && (xhtml.indexOf("<body") < xhtml.indexOf("</body>"))){
		pdiv.innerHTML = "<div>" + xhtml.substring(xhtml.indexOf("<body"), xhtml.indexOf("</body>")+7) + "</div>";
		cleanxml = getContents(contentDom.childNodes[0], whitelist_elements);
		cleanxml = cleanxml.substring(5, cleanxml.length-7);
 		cleanxml = xhtml.substring(0, xhtml.indexOf("<body")) + xhtml.match("<body[^>]*>") + "\n" + cleanxml + xhtml.substring(xhtml.indexOf("</body>"));
	}else{
		cleanxml = getContents(contentDom.childNodes[0], whitelist_elements);
	}

	pdiv.innerHTML = '';

	return cleanxml;
}

function getContents(dom, whitelist_elements){
    var nodeXML = '';
    var i, j;
    var containingData='';
    
    // Gather node information
    var elementType = dom.nodeName; if(!elementType) elementType = '';
    var elementValue = dom.nodeValue; if(!elementValue) elementValue = '';
    elementType = elementType.toLowerCase();
    
    // Special types
    if(elementType == '#text'){
	return elementValue;
    }
    if(elementType == '#comment'){
        nodeXML += "<!--" + elementValue + "-->";
        return(nodeXML);
    }
    
    // Check if tags are in whitelist
    var allowtag = false;
    var tagnr=-1;
    for(i=0;i<whitelist_elements.length;i++)
        if(dom.tagName.toLowerCase()==whitelist_elements[i][0].toLowerCase()){
            allowtag=true;
            tagnr=i;
            break;
        }
    
    // Get nodes
    for(i=0;i<dom.childNodes.length;i++){
        containingData += getContents(dom.childNodes[i], whitelist_elements);
    }
    
    // Removal of empty tags
    var isEmpty = containingData==''; // Check if it is only spaces
    if(allowtag) if(isEmpty) if(whitelist_elements[tagnr][2]) return ''; // Remove empty tags which should have contents
    if(allowtag){// Remove tags where no attributes and which should be removed without
        var attrs = 0;
        for(i=0;i<dom.attributes.length;i++)
            for(j=0;j<whitelist_elements[tagnr][1].length;j++)
                if(dom.attributes[i].value)
                    if(dom.attributes[i].name.toLowerCase()==whitelist_elements[tagnr][1][j].toLowerCase()){
                        attrs++;
                    }
        if(attrs==0) if(whitelist_elements[tagnr][3]) return containingData;
    }
    
    // Filter
    if(allowtag){
        nodeXML += "<" + elementType;
        
        // Attributes
        for(i=0;i<dom.attributes.length;i++){
            var allowattr=false;
            
            for(j=0;j<whitelist_elements[tagnr][1].length;j++)
                if(dom.attributes[i].name.toLowerCase()==whitelist_elements[tagnr][1][j].toLowerCase()){
                    allowattr=true;
                    break;
                }
            
            if(allowattr){
                if(dom.attributes[i].value=='null') continue;
                if(dom.attributes[i].value==null) continue;
                if(dom.attributes[i].value=='') continue;
                nodeXML += " " + dom.attributes[i].name + '="' + dom.attributes[i].value + '"';
            }
        }
        
        if(!isEmpty) nodeXML += ">";
        else nodeXML += "/>";
    }
    
    nodeXML += containingData;
    
    if(allowtag && !isEmpty){
        nodeXML += "</" + elementType + ">";
    }
    
    return nodeXML;
}