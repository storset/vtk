<#ftl strip_whitespace=true>

<#--
  - File: fckeditor.ftl
  - 
  - Required model data:
  -  
  - Optional model data:
  -
  -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
        <title>FCKeditor</title>
    </head>
    <body>

        <p>A: Editor for HTML 1 Transitional</p>
        Config: 
        <pre>
          fckeditorBase: ${fckeditorBase.url?html}
          fckSource.getURL: ${fckSource.getURL?html}
          fckSource.putURL: ${fckSource.putURL?html}
        </pre>


        
        <div id="myEditorDiv">hello</div>
        <script type="text/javascript" src="${fckeditorBase.url?html}/fckeditor.js"></script>
	<script type="text/javascript">
		function createFCKEditorInDiv(editorDiv, contents, w, h, editorinstancename){
		    var div = document.getElementById(editorDiv);
		    var fck = new FCKeditor(editorinstancename, w, h);
		    fck.BasePath = "${fckeditorBase.url?html}/";
		    fck.Value = contents;
                    fck.Config['FullPage'] = true ;
		    div.innerHTML = fck.CreateHtml();
		}
		    
		// [[Tag, [Attribute, ...], <Remove if empty>, <Remove if no attributes>], ...]
		// xhtml1-transitional
		var whitelist = [
		    ["html", ["lang", "xml:lang", "dir", "id", "xmlns"]],
		    ["head", ["lang", "xml:lang", "dir", "id", "profile"]],
		    ["title", ["lang", "xml:lang", "dir", "id"]],
		    ["base", ["id", "href", "target"]],
		    ["meta", ["lang", "xml:lang", "dir", "id", "http-equiv", "name", "content", "scheme"]],
		    ["link", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "charset", "href", "hreflang", "type", "rel", "rev", "media", "target"]],
		    ["style", ["lang", "xml:lang", "dir", "id", "type", "media", "title", "xml:space"]],
		    ["script", ["id", "charset", "type", "language", "src", "defer", "xml:space"]],
		    ["noscript", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["iframe", ["id", "class", "style", "title", "longdesc", "name", "src", "frameborder", "marginwidth", "marginheight", "scrolling", "align", "height", "width"]],
		    ["noframes", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["body", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "onload", "onunload", "background", "bgcolor", "text", "link", "vlink", "alink"]],
		    ["div", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["p", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h1", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h2", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h3", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h4", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h5", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["h6", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["ul", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "type", "compact"]],
		    ["ol", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "type", "compact", "start"]],
		    ["menu", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "compact"]],
		    ["dir", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "compact"]],
		    ["li", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "type", "value"]],
		    ["dl", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "compact"]],
		    ["dt", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["dd", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["address", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["hr", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align", "noshade", "size", "width"]],
		    ["pre", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "width", "xml:space"]],
		    ["blockquote", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "cite"]],
		    ["center", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["ins", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "cite", "datetime"]],
		    ["del", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "cite", "datetime"]],
		    ["a", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "tabindex", "onfocus", "onblur", "charset", "type", "name", "href", "hreflang", "rel", "rev", "shape", "coords", "target"]],
		    ["span", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["bdo", ["id", "class", "style", "title", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "lang", "xml:lang", "dir"]],
		    ["br", ["id", "class", "style", "title", "clear"]],
		    ["em", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["strong", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["dfn", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["code", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["samp", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["kbd", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["var", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["cite", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["abbr", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["acronym", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["q", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "cite"]], 
		    ["sub", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["sup", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["tt", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["i", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["b", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["big", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["small", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["u", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["s", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["strike", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["basefont", ["id", "size", "color", "face"]],
		    ["font", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "size", "color", "face"]],
		    ["object", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "declare", "classid", "codebase", "data", "type", "codetype", "archive", "standby", "height", "width", "usemap", "name", "tabindex", "align", "border", "hspace", "vspace"]],
		    ["param", ["id", "name", "value", "valuetype", "type"]],
		    ["applet", ["id", "class", "style", "title", "codebase", "archive", "code", "object", "alt", "name", "width", "height", "align", "hspace", "vspace"]],
		    ["img", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "src", "alt", "name", "longdesc", "height", "width", "usemap", "ismap", "align", "border", "hspace", "vspace"]],
		    ["map", ["lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "id", "class", "style", "title", "name"]],
		    ["area", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "tabindex", "onfocus", "onblur", "shape", "coords", "href", "nohref", "alt", "target"]],
		    ["form", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "action", "method", "name", "enctype", "onsubmit", "onreset", "accept", "accept-charset", "target"]],
		    ["label", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "for", "accesskey", "onfocus", "onblur"]],
		    ["input", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "tabindex", "onfocus", "onblur", "type", "name", "value", "checked", "disabled", "readonly", "size", "maxlength", "src", "alt", "usemap", "onselect", "onchange", "accept", "align"]],
		    ["select", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "name", "size", "multiple", "disabled", "tabindex", "onfocus", "onblur", "onchange"]],
		    ["optgroup", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "disabled", "label"]],
		    ["option", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "selected", "disabled", "label", "value"]],
		    ["textarea", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "tabindex", "onfocus", "onblur", "name", "rows", "cols", "disabled", "readonly", "onselect", "onchange"]],
		    ["fieldset", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup"]],
		    ["legend", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "align"]],
		    ["button", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "accesskey", "tabindex", "onfocus", "onblur", "name", "value", "type", "disabled"]],
		    ["isindex", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "prompt"]],
		    ["table", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "summary", "width", "border", "frame", "rules", "cellspacing", "cellpadding", "align", "bgcolor"]],
		    ["caption", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align"]],
		    ["colgroup", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "span", "width", "align", "char", "charoff", "valign"]],
		    ["col", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "span", "width", "align", "char", "charoff", "valign"]],
		    ["thead", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align", "char", "charoff", "valign"]],
		    ["tfoot", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align", "char", "charoff", "valign"]],
		    ["tbody", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align", "char", "charoff", "valign"]],
		    ["tr", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "align", "char", "charoff", "valign", "bgcolor"]],
		    ["th", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "abbr", "axis", "headers", "scope", "rowspan", "colspan", "align", "char", "charoff", "valign", "nowrap", "bgcolor", "width", "height"]],
		    ["td", ["id", "class", "style", "title", "lang", "xml:lang", "dir", "onclick", "ondblclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove", "onmouseout", "onkeypress", "onkeydown", "onkeyup", "abbr", "axis", "headers", "scope", "rowspan", "colspan", "align", "char", "charoff", "valign", "nowrap", "bgcolor", "width", "height"]]
		];

		function getXmlHttpRequestObject(){
			try{
				var ret = new ActiveXObject("Microsoft.XMLHTTP");
				return ret;
			}catch(e){
				return new XMLHttpRequest();
			}
		}

		// The following code is executed on load
		var xReq = getXmlHttpRequestObject(); // new XMLHttpRequest()
		xReq.onreadystatechange = function(){ if(xReq.readyState==4){
			//alert(xReq.responseText);
			createFCKEditorInDiv("myEditorDiv", xReq.responseText, 700, 500, "myEditorIstance");
		}}
		xReq.open("get", "${fckSource.getURL}", true);
		xReq.send(null);

		// Save function
		function performSave(reshtml){
			var xReq = getXmlHttpRequestObject(); // new XMLHttpRequest()
			xReq.open("get", "${fckSource.getURL}", true);
			xReq.send(reshtml);
		}
	</script>
        
    </body>
</html>
