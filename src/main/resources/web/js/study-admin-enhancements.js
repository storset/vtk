function hideShowStudy(typeToDisplay){
        	switch(typeToDisplay){
                case "so" :
                    $('.frist-frekvens').show();
                    $('.metode').show();
                    $('.internasjonale-sokere').hide();
                    $('.nordiske-sokere').hide();
                    $('.opptakskrav').show();
                    $('.generelle').hide();
                    $('.studiekode').show();
                    $('.pris').hide();
                    $('.regelverk').hide();
                break;
                case "nm" : 
                    $('.frist-frekvens').show();
                    $('.metode').show();
                    $('.internasjonale-sokere').hide();
                    $('.nordiske-sokere').hide();
                    $('.opptakskrav').hide();
                    $('.generelle').show();
                    $('.studiekode').hide();
                    $('.pris').show();
                    $('.regelverk').show();
                break;
                case "em" :
                    $('.frist-frekvens').hide();
                    $('.metode').hide();
                    $('.internasjonale-sokere').show();
                    $('.nordiske-sokere').show();
                    $('.opptakskrav').hide();
                    $('.generelle').show();
                    $('.studiekode').hide();
                    $('.pris').show();
                    $('.regelverk').show();
                break;
                default :
                    $('.frist-frekvens').show();
                    $('.metode').show();
                    $('.internasjonale-sokere').show();
                    $('.nordiske-sokere').show();
                    $('.opptakskrav').show();
                    $('.generelle').show();
                    $('.studiekode').show();
                    $('.pris').show();
                    $('.regelverk').show();
                break;
            }
        }

$(document).ready(function() {
    try{
       	var typeToDisplay = $('#typeToDisplay').val();
            hideShowStudy(typeToDisplay);
        } catch (err){
        	return false;
        }
        $('#typeToDisplay').change(function() {
           var typeToDisplay = $('#typeToDisplay').val();
           hideShowStudy(typeToDisplay);
          });
        });