/*****************************************************************************
 *
 * Copyright (c) 2003-2005 Kupu Contributors. All rights reserved.
 *
 * This software is distributed under the terms of the Kupu
 * License. See LICENSE.txt for license text. For a list of Kupu
 * Contributors see CREDITS.txt.
 *
 *****************************************************************************/

// $Id: kupusaveonpart.js 9879 2005-03-18 12:04:00Z yuppie $

var iframeid = "kupu-editor";

function saveOnPart() {
    // ask the user if (s)he wants to save the document before leaving
    if( kupu.content_changed ) {
        if( confirm(_('You are leaving this page. \n Press OK to save or CANCEL to discard unsaved changes.')) ) {
            kupu.config.reload_src = 0;
            kupu.saveDocument(false, true);
        }
    };
    
    // ensure that latest version of document is cached by web browser before leaving edit window
    // (to avoid browser fetching old version of the document if 'back' button is used)
    var editIframe = document.getElementById(iframeid);
    if (!editIframe) {
        return false;
    }
    else {
        editIframe.src = editIframe.src;
    }    
};
