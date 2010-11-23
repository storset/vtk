function getCkValue(instanceName) {
    var oEditor = getCkInstance(instanceName);
    return oEditor.getData();
}

function getCkInstance(instanceName){
  	for(var i in CKEDITOR.instances) {
    	if(CKEDITOR.instances[i].name == instanceName){
    		return CKEDITOR.instances[i];
	  	} 
    }
    return null;
}

function setCkValue(instanceName, data) {
    var oEditor = getCkInstance(instanceName);
    oEditor.setData(data);
}
   
function isCkEditor(instanceName) {
    var oEditor = getCkInstance(instanceName);
    return oEditor != null;
}