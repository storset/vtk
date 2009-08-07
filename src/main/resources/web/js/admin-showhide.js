// JavaScript Document  
function setShowHide(name, parameters){
    $("#" + name + "-true").each( 
            function(){
                if(this.checked){
                    for(i = 0;i<parameters.length;i++){
                        $("div." + parameters[i]).hide();
                    }
                }
            }
    );     
    $("#" + name + "-false").each( 
            function(){
                if(this.checked){
                    for(i = 0;i<parameters.length;i++){
                        $("div." + parameters[i]).show();
                    }
                }
            }
    );
}