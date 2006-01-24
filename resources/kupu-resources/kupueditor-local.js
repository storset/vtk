

/**
 * Override PropertyTool: don't insert <meta name="description" ...>
 * elements into the document. Originally defined in 'kupubasetools.js'.
 */
function PropertyTool(titlefieldid, descfieldid) {
    /* The property tool */

    this.titlefield = getFromSelector(titlefieldid);
    this.descfield = getFromSelector(descfieldid);

    this.initialize = function(editor) {
        /* attach the event handlers and set the initial values */
        this.editor = editor;
        addEventHandler(this.titlefield, "change", this.updateProperties, this);
        addEventHandler(this.descfield, "change", this.updateProperties, this);
        
        // set the fields
        var heads = this.editor.getInnerDocument().getElementsByTagName('head');
        if (!heads[0]) {
            this.editor.logMessage(_('No head in document!'), 1);
        } else {
            var head = heads[0];
            var titles = head.getElementsByTagName('title');
            if (titles.length) {
                this.titlefield.value = titles[0].text;
            }
            var metas = head.getElementsByTagName('meta');
            if (metas.length) {
                for (var i=0; i < metas.length; i++) {
                    var meta = metas[i];
                    if (meta.getAttribute('name') && 
                            meta.getAttribute('name').toLowerCase() == 
                            'description') {
                        this.descfield.value = meta.getAttribute('content');
                        break;
                    }
                }
            }
        }

        this.editor.logMessage(_('Property tool initialized'));
    };

    this.updateProperties = function() {
        /* event handler for updating the properties form */
        var doc = this.editor.getInnerDocument();
        var heads = doc.getElementsByTagName('HEAD');
        if (!heads) {
            this.editor.logMessage(_('No head in document!'), 1);
            return;
        }

        var head = heads[0];

        // set the title
        var titles = head.getElementsByTagName('title');
        if (!titles) {
            var title = doc.createElement('title');
            var text = doc.createTextNode(this.titlefield.value);
            title.appendChild(text);
            head.appendChild(title);
        } else {
            var title = titles[0];
            // IE6 title has no children, and refuses appendChild.
            // Delete and recreate the title.
            if (title.childNodes.length == 0) {
                title.removeNode(true);
                title = doc.createElement('title');
                title.innerText = this.titlefield.value;
                head.appendChild(title);
            } else {
                title.childNodes[0].nodeValue = this.titlefield.value;
            }
        }
        document.title = this.titlefield.value;

        // let's just fulfill the usecase, not think about more properties
        // set the description
        /*
        var metas = doc.getElementsByTagName('meta');
        var descset = 0;
        for (var i=0; i < metas.length; i++) {
            var meta = metas[i];
            if (meta.getAttribute('name') && 
                    meta.getAttribute('name').toLowerCase() == 'description') {
                meta.setAttribute('content', this.descfield.value);
            }
        }

        if (!descset) {
            var meta = doc.createElement('meta');
            meta.setAttribute('name', 'description');
            meta.setAttribute('content', this.descfield.value);
            head.appendChild(meta);
        }

        */
        this.editor.logMessage(_('Properties modified'));
    };
}

PropertyTool.prototype = new KupuTool;







/**
 * This function gets called from the onload event, after startKupu().
 */
function kupuLocalHook(kupu) {

    kupu._serializeOutputToString = function(transform) {
        // XXX need to fix this.  Sometimes a spurious "\n\n" text 
        // node appears in the transform, which breaks the Moz 
        // serializer on .xml
            
        if (this.config.strict_output) {
            var contents =  '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" ' + 
            '"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">\n' + 
            '<html xmlns="http://www.w3.org/1999/xhtml">' +
            Sarissa.serialize(transform.getElementsByTagName("head")[0]) +
            Sarissa.serialize(transform.getElementsByTagName("body")[0]) +
            '</html>';
        } else {
            var contents = '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" ' + 
            '"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n' + 
            '<html xmlns="http://www.w3.org/1999/xhtml">' +
            Sarissa.serialize(transform.getElementsByTagName("head")[0]) +
            Sarissa.serialize(transform.getElementsByTagName("body")[0]) +
            '</html>';
        };

        contents = this.escapeEntities(contents);

        if (this.config.compatible_singletons) {
            contents = this._fixupSingletons(contents);
        };
        
        return contents;
    };
}
