/**
 * @file Generic form related functions should go in here
 * @author kehan
 */

bdrs.form = {};
/**
 * Automatically populates form fields with values from query parameters in the URL
 */
bdrs.form.prepopulate = function(){
	var fields = jQuery("form").serializeArray();
	for ( var i = 0; i < fields.length; i++) {
		var field = fields[i];
		if(field.value == ""){
		var value = bdrs.getParameterByName(field.name);
		var selector = '[name="' + field.name +'"]';
		jQuery(selector).filter(function(){
			/**
			 * Exclude any fields we don't want to using css
			 */
			return !$(this).hasClass("skipPrepopulate", "bdrsPrepopulated");
		}).val(value).addClass("bdrsPrepopulated");
		}
	}
	
	// update the date field to the current date last if it has not already been set
    if (jQuery("#date").val() != undefined && jQuery("#date").val().length === 0) {
    	jQuery("#date").val(bdrs.util.formatDate(new Date()));
    }
};

bdrs.form.not_validated_form_selectors = [];
bdrs.form.remove_from_validation = function(selector) {
    if(selector !== undefined && selector !== null) {
        bdrs.form.not_validated_form_selectors.push(selector);
    }
}

bdrs.form.init_form_validation = function() {
    var form_list = jQuery('form');
    if(bdrs.form.not_validated_form_selectors.length > 0) {
        form_list = form_list.not(bdrs.form.not_validated_form_selectors.join(','));
    }

    form_list.ketchup({attribute:'class'});
}
