function hideShowStudy(typeToDisplay){
        	switch(typeToDisplay){
                case "so" :
                    $('.frist-frekvens-admin').show();
                    $('.metode-admin').show();
                    $('.internasjonale-sokere-admin').hide();
                    $('.nordiske-sokere').hide();
                    $('.opptakskrav-admin').show();
                    $('.generelle-admin').hide();
                    $('.studiekode').show();
                    $('.pris').hide();
                    $('.regelverk').hide();
                break;
                case "nm" : 
                    $('.frist-frekvens-admin').show();
                    $('.metode-admin').show();
                    $('.internasjonale-sokere-admin').hide();
                    $('.nordiske-sokere').hide();
                    $('.opptakskrav-admin').hide();
                    $('.generelle-admin').show();
                    $('.studiekode').hide();
                    $('.pris').show();
                    $('.regelverk').show();
                break;
                case "em" :
                    $('.frist-frekvens-admin').hide();
                    $('.metode-admin').hide();
                    $('.internasjonale-sokere-admin').show();
                    $('.nordiske-sokere').show();
                    $('.opptakskrav-admin').hide();
                    $('.generelle-admin').show();
                    $('.studiekode').hide();
                    $('.pris').show();
                    $('.regelverk').show();
                break;
                default :
                    $('.frist-frekvens-admin').show();
                    $('.metode-admin').show();
                    $('.internasjonale-sokere-admin').show();
                    $('.nordiske-sokere').show();
                    $('.opptakskrav-admin').show();
                    $('.generelle-admin').show();
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
        $(document).on('change', '#typeToDisplay', function() {
           var typeToDisplay = $('#typeToDisplay').val();
           hideShowStudy(typeToDisplay);
          });
        });