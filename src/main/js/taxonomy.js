/*global jQuery: false OpenLayers: false */

bdrs.taxonomy = {};

bdrs.taxonomy.handlers = {};

/**
 * Initialises the taxonomy listing screen by binding an autocomplete
 * widget to the taxonomy search input. 
 *
 * @param taxonAutocompleteSelector the jQuery selector for the input where 
 * the taxonomy search autocomplete shall be bound.
 * @param selectedTaxonPkSelector the input where the primary key of the 
 * selected taxon shall be stored.
 * @param taxonPropertiesContainerSelector the selector to the containing 
 * element where properties of the taxon shall be inserted.
 * @param editTaxonSelector the selector to the element to be enabled when a 
 * taxon is selected.
 * @param groupAutocompleteSelector the selector of the element that to have
 * the taxon group autocomplete attached
 * @param groupPkSelector the selector of the element to put the taxon group
 * primary key when one has been selected from the autocomplete.
 * @param buttonPanelSelector the selector for the button panel that is rendered
 * underneath the taxon profile when it becomes visible.
 */
bdrs.taxonomy.initListing = function(taxonAutocompleteSelector,
                                     selectedTaxonPkSelector,
                                     taxonPropertiesContainerSelector,
                                     editTaxonSelector,
                                     groupAutocompleteSelector,
                                     groupPkSelector,
                                     buttonPanelSelector) {

    jQuery(taxonAutocompleteSelector).autocomplete({
        source: function(request, callback) {
            var params = {};
            params.q = request.term;
            params.depth = 2;

            jQuery.getJSON(bdrs.portalContextPath+'/webservice/taxon/searchTaxon.htm', params, function(data, textStatus) {
                var label;
                var result;
                var taxon;
                var resultsArray = [];
                for(var i=0; i<data.length; i++) {
                    taxon = data[i];

                    label = [];
                    if(taxon.scientificName !== undefined && taxon.scientificName.length > 0) {
                        label.push("<b><i>"+taxon.scientificName+"</b></i>");
                    }
                    if(taxon.commonName !== undefined && taxon.commonName.length > 0) {
                        label.push(taxon.commonName);
                    }

                    label = label.join(' ');

                    resultsArray.push({
                        label: label,
                        value: taxon.scientificName,
                        data: taxon
                    });
                }

                callback(resultsArray);
            });
        },
        select: function(event, ui) {
            var taxon = ui.item.data;
            bdrs.taxonomy.displayTaxonProperties(taxon,
                                                 taxonAutocompleteSelector,
                                                 selectedTaxonPkSelector,
                                                 taxonPropertiesContainerSelector,
                                                 editTaxonSelector,
                                                 buttonPanelSelector);
        },
        html: true,
        minLength: 2,
        delay: 300
    });
    
    bdrs.taxonomy.initTaxonGroupAutocomplete(groupAutocompleteSelector, groupPkSelector);
    
    html5media.configureFlowplayer = function (tag, element, config) {
        if(tag === 'audio') {
            config.clip.type = 'audio';
        }
        config.plugins.controls.all = false;
        config.plugins.controls.play = true;
        config.plugins.controls.scrubber = true;
        config.plugins.controls.volume = true;
    }
};

/**
 * Initialises the taxon group auto complete input
 * 
 * @param {Object} groupAutocompleteSelector - selector for the text input that the user types in
 * @param {Object} groupPkSelector - selector for the hidden input that is filled by the taxon group pk
 */
bdrs.taxonomy.initTaxonGroupAutocomplete = function(groupAutocompleteSelector, groupPkSelector) {
	// Taxon Group autocomplete.
    jQuery(groupAutocompleteSelector).autocomplete({
        source: function(request, callback) {
            var params = {};
            params.q = request.term;

            jQuery.getJSON(bdrs.portalContextPath+'/webservice/taxon/searchTaxonGroup.htm', params, function(data, textStatus) {
                var label;
                var result;
                var taxonGroup;
                var resultsArray = [];
                for(var i=0; i<data.length; i++) {
                    taxonGroup = data[i];
                    resultsArray.push({
                        label: taxonGroup.name,
                        value: taxonGroup.name,
                        data: taxonGroup
                    });
                }

                callback(resultsArray);
            });
        },
        select: function(event, ui) {
            var taxonGroup = ui.item.data;
            jQuery(groupPkSelector).val(taxonGroup.id);
        },
        html: true,
        minLength: 2,
        delay: 300
    });
};

/**
 * Displays the properties of the specified taxon. This function will 
 * automatically perform necessary ajax requests to retrieve the name of the
 * taxon group and the name of the parent taxon.
 *
 * @param taxon a representation of a taxon generated by the taxon webservice. 
 * @param taxonAutocompleteSelector the jQuery selector for the input where 
 * the taxonomy search autocomplete shall be bound.
 * @param selectedTaxonPkSelector the input where the primary key of the 
 * selected taxon shall be stored.
 * @param taxonPropertiesContainerSelector the selector to the containing 
 * element where properties of the taxon shall be inserted.
 * @param editTaxonSelector the selector to the element to be enabled when a 
 * taxon is selected.
 * @param buttonPanelSelector the selector for the button panel that is rendered
 * underneath the taxon profile when it becomes visible.
 */
bdrs.taxonomy.displayTaxonProperties = function(taxon,
                                                taxonAutocompleteSelector,
                                                selectedTaxonPkSelector,
                                                taxonPropertiesContainerSelector,
                                                editTaxonSelector,
                                                buttonPanelSelector) {
    jQuery(selectedTaxonPkSelector).val(taxon.id);
    jQuery(editTaxonSelector).removeAttr("disabled");
    
    var propertiesElem = jQuery(taxonPropertiesContainerSelector);
    propertiesElem.empty();
    
    var rows = [];
    // Common and scientific name
    rows.push('<tr><th colspan="2">' +
                taxon.commonName +
                '</th><th colspan="2" class="scientificName">' +
                taxon.scientificName +
                '</th></tr>');
    // Rank                
    rows.push('<tr><td>Rank:</td><td colspan="3">' +
                titleCaps(taxon.taxonRank.toLowerCase()) +
                '</td></tr>');

    
    if(taxon.parent !== null) {
        // Parent
        rows.push('<tr><td>Parent:</td><td colspan="3"><a class="taxonParent" href="javascript:void(0)">' +
                    taxon.parent.commonName +
                    '</a>&nbsp;<a href="javascript:void(0)" class="scientificName taxonParent">' +
                    taxon.parent.scientificName +
                    '</a></td></tr>');
    }

    // Group
    rows.push('<tr><td>Group:</td><td colspan="3">'+taxon.taxonGroup.name+'</td></tr>');

    // Secondary groups
    if (taxon.secondaryGroups != null) {
        var secondaryGroups = '';
        for (group in taxon.secondaryGroups) {
            if (secondaryGroups !== '') {
                secondaryGroups += ', ';
            }
            secondaryGroups += taxon.secondaryGroups[group].name;

        }

        rows.push('<tr><td>Secondary Groups:</td><td colspan="3">'+secondaryGroups+'</td></tr>');
    }
    
    // Update
    var datestr = taxon._updatedAt_formatted;
    rows.push('<tr><td>Update:</td><td colspan="3">'+datestr+'</td></tr>');
    
    var propertiesTable = jQuery("<table><tbody>"+rows.join('')+"</tbody></table>");
    propertiesTable.addClass("taxonPropertiesTable");
    propertiesElem.append(propertiesTable);
    
    // Attach the parent taxon click handlers
    jQuery(".taxonParent").click(function() {
        jQuery.getJSON(bdrs.portalContextPath+'/webservice/taxon/getTaxonById.htm', {id: taxon.parent.id, depth: 2}, function(parentTaxon) {
            jQuery(taxonAutocompleteSelector).val(parentTaxon.scientificName);
	        bdrs.taxonomy.displayTaxonProperties(parentTaxon,
	                                             taxonAutocompleteSelector,
	                                             selectedTaxonPkSelector,
	                                             taxonPropertiesContainerSelector,
	                                             editTaxonSelector);
         });
    });
    
    // Taxon Profile
    var taxonProfileRows = [];
    var taxonProfile;
    for(var pIndex=0; pIndex<taxon.infoItems.length; pIndex++) {
        taxonProfile = taxon.infoItems[pIndex];
        
        var content;
        if(taxonProfile.fileType) {
            var url = bdrs.portalContextPath+'/files/downloadByUUID.htm?uuid='+taxonProfile.content;
            if(taxonProfile.audioType) {
                var player = jQuery("<audio></audio>").attr({src:url, controls:"controls"}).text(taxonProfile.content);
                content = (jQuery('<span>').append(player).clone()).remove().html();
            } else {
	            var link = jQuery("<a></a>");
	            link.attr({href: url});
	            link.text(taxonProfile.content);
	            
	            if(taxonProfile.imgType) {
	                link.text("");
	                var img = jQuery("<img/>").attr({src:url}).addClass("max_size_img");
	                link.append(img);                
	            }
	            content = (jQuery('<span>').append(link).clone()).remove().html();
            }
        
        } else {
            content = taxonProfile.content;
        }
        
        // Type Content Description Header
        taxonProfileRows.push('<tr><td>' + taxonProfile.type +
                              '</td><td>' + taxonProfile.header + 
                              '</td><td>' + taxonProfile.description +
                              '</td><td>' + content + '</td></tr>');
    }
    if(taxonProfileRows.length > 0) {
	    var profileTable = jQuery('<table><thead><tr>' +
	                              '<th>Type</th><th>Database Name</th><th>Title</th><th>Content</th></tr></thead>'+
	                              '<tbody>'+taxonProfileRows.join('')+'</tbody></table>');
	    profileTable.addClass('datatable textcenter');
	    propertiesElem.append('<h3>Taxon Profile</h3>').append(profileTable);
	    
	    // Append flash audio player if necessary.
	    html5media();
    }
    
    // Group Attributes
    // Be aware that the IndicatorSpeciesAttributes on the IndicatorSpecies
    // could be for more than one group. We only want to display the
    // attributes for the attached group.
    var attrVal;
    // { Attribute.id : IndicatorSpeciesAttribute }
    var attrToAttrValMap = {};
    for(var attrValIndex=0; attrValIndex < taxon.attributes.length; attrValIndex++) {
        attrVal = taxon.attributes[attrValIndex];
        attrToAttrValMap[attrVal.attribute.id] = attrVal;
    }
    
    var groupAttr;
    var displayElem;
    var groupAttrRows = [];
    for(var groupAttrIndex=0; 
        groupAttrIndex < taxon.taxonGroup.attributes.length; 
        groupAttrIndex++) {
        
        groupAttr = taxon.taxonGroup.attributes[groupAttrIndex];
        // If there is no data, do not show anything
        if(attrToAttrValMap[groupAttr.id] !== undefined) {
            
            attrVal = attrToAttrValMap[groupAttr.id];
            var attr_type = bdrs.model.taxa.attributeType.code[groupAttr.typeCode]
            if(groupAttr.type === 'FILE' || groupAttr.type === 'AUDIO' || groupAttr.type === 'VIDEO') {
                displayElem = '<a href="'+bdrs.portalContextPath+'/files/download.htm?'+attrVal.fileURL+'">' +
                                attrVal.stringValue + '</a>';
            } else if(groupAttr.type === 'IMAGE') {
                displayElem = '<a href="'+bdrs.portalContextPath+'/files/download.htm?'+attrVal.fileURL+'">' +
                                '<img class="max_size_img" src="' +
                                bdrs.portalContextPath+'/files/download.htm?'+attrVal.fileURL +
                                '" alt="Missing Image"/></a>';
            } else if (attr_type.isCensusMethodType()) {
                displayElem = bdrs.attribute.createCensusMethodTable(attr_type, attrVal, false).html();
            } else {
                displayElem = attrVal.stringValue;
            }
            
            groupAttrRows.push('<tr><th class="textright"><label>' + 
            groupAttr.description +
            '</label></th><td>' +
            displayElem +
            '</td></tr>');                    
        }
    }
    
    if(groupAttrRows.length > 0) {
	    var groupAttrTable = jQuery('<table><tbody>'+groupAttrRows.join('')+'</tbody></table>');
	    groupAttrTable.addClass('datatable');
	    propertiesElem.append('<h3>Group Attributes</h3>').append(groupAttrTable);
    }
    jQuery(buttonPanelSelector).show();
};

/**
 * Provides functionality to the autocomplete that works with taxon groups.
 * Taxon groups returned from the server based on the typed text are additionally filtered to remove groups
 * that have already been assigned.
 */
bdrs.taxonomy.taxonGroupSearcher = {

    primaryGroupSelector: '#taxonGroupPk',
    secondaryGroupSelector: '.secondaryGroupId',
    excludedGroups: [],

    updateExcludedGroups : function() {
        var groups = [];
        groups.push(parseInt(jQuery(bdrs.taxonomy.taxonGroupSearcher.primaryGroupSelector).val()));

        jQuery(bdrs.taxonomy.taxonGroupSearcher.secondaryGroupSelector).each(function() {
            groups.push(parseInt(jQuery(this).val()));
        });
        return groups;
    },
    initSearch : function(event, ui) {

        var excluded = bdrs.taxonomy.taxonGroupSearcher.updateExcludedGroups();
        var currentElementValue = jQuery(event.target).next('input[type="text"]').val();
        currentElementValue = parseInt(currentElementValue);

        var index = jQuery.inArray(currentElementValue, excluded);
        if (index >= 0) {
            excluded.splice(index, 1);
        }
        bdrs.taxonomy.taxonGroupSearcher.excludedGroups = excluded;
    },

    searchTaxonGroups : function(request, callback) {

        var params = {};
        params.q = request.term;

        jQuery.getJSON(bdrs.portalContextPath+'/webservice/taxon/searchTaxonGroup.htm', params, function(data, textStatus) {

            var taxonGroup;
            var resultsArray = [];
            for(var i=0; i<data.length; i++) {
                taxonGroup = data[i];

                if (jQuery.inArray(taxonGroup.id, bdrs.taxonomy.taxonGroupSearcher.excludedGroups) < 0) {

                    resultsArray.push({
                        label: taxonGroup.name,
                        value: taxonGroup.name,
                        data: taxonGroup
                    });
                }
            }

            callback(resultsArray);
        });
    }
};

/**
 * Deletes a row from the secondary groups table, making sure ketchup is happy first.
 * @param event the click event that triggered this delete.
 */
bdrs.taxonomy.deleteSecondaryGroup = function(event) {

    var rowSelector = 'tr:first';
    var inputSelector = 'input[name=secondaryGroups]';
    // Yet another crazy ketchup workaround.
    var hiddenField = jQuery(event.target).parents(rowSelector).find(inputSelector);
    hiddenField.val("-1");
    hiddenField.focus();

    jQuery(this).parents(rowSelector).remove();
};

/**
 * Initialises the edit taxon screen by binding autocomplete widgets to the
 * taxon parent input and the taxon group input.
 *
 * @param parentAutocompleteSelector the jQuery selector to the input where
 * the autocomplete for the parent taxon search shall be bound.
 * @param parentPkSelector  the jQuery selector to the input where the primary
 * key of the parent taxon shall be stored once selected.
 * @param groupAutocompleteSelector the jQuery selector to the input where
 * the autocomplete for the taxon group shall be bound.
 * @param groupPkSelector the jQuery selector to the input where the primary
 * key of the taxon group shall be stored once selected.
 * @param taxonPkSelector the jQuery selector for the element containing the
 * primary key of the current taxon. Note that this selector may return
 * zero elements if the taxon is new and hence does not have a primary key.
 * @param taxonAttributeWrapperSelector the jQuery selector for the element 
 */
bdrs.taxonomy.initEditTaxon = function(parentAutocompleteSelector, parentPkSelector,
                                       groupAutocompleteSelector, groupPkSelector,
                                       taxonPkSelector, taxonAttributeWrapperSelector,
                                       taxonProfileTableSelector, newProfileIndexSelector,
                                       secondaryGroupAutocompleteSelector) {
    // Parent Taxon
    jQuery(parentAutocompleteSelector).autocomplete({
        source: function(request, callback) {
            var params = {};
            params.q = request.term;

            jQuery.getJSON(bdrs.portalContextPath+'/webservice/taxon/searchTaxon.htm', params, function(data, textStatus) {
                var label;
                var result;
                var taxon;
                var resultsArray = [];
                for(var i=0; i<data.length; i++) {
                    taxon = data[i];

                    label = [];
                    if(taxon.scientificName !== undefined && taxon.scientificName.length > 0) {
                        label.push("<b><i>"+taxon.scientificName+"</b></i>");
                    }
                    if(taxon.commonName !== undefined && taxon.commonName.length > 0) {
                        label.push(taxon.commonName);
                    }

                    label = label.join(' ');

                    resultsArray.push({
                        label: label,
                        value: taxon.scientificName,
                        data: taxon
                    });
                }

                callback(resultsArray);
            });
        },
        select: function(event, ui) {
            var parent = ui.item.data;
            jQuery(parentPkSelector).val(parent.id);
        },
        change: function(event, ui) {
            if(ui.item === null) {
                jQuery(parentPkSelector).val('');
                jQuery(parentAutocompleteSelector).val('');
            }
        },
        html: true,
        minLength: 2,
        delay: 300
    });

    // Taxon Group
    jQuery(groupAutocompleteSelector).autocomplete({
        search: bdrs.taxonomy.taxonGroupSearcher.initSearch,
        source: bdrs.taxonomy.taxonGroupSearcher.searchTaxonGroups,
        select: function(event, ui) {
            var taxonGroup = ui.item.data;
            jQuery(groupPkSelector).val(taxonGroup.id);

            // Update the taxon attribute table
            jQuery(taxonAttributeWrapperSelector).empty();
            var param = {};

            param.groupPk = jQuery(groupPkSelector).val();
            var taxonPkElem = jQuery(taxonPkSelector);
            if(taxonPkElem.length > 0) {
                param.taxonPk = taxonPkElem.val();
            }
            
            jQuery.get(bdrs.portalContextPath+'/bdrs/admin/taxonomy/ajaxTaxonAttributeTable.htm', param, function(data) {
                var taxonAttributeWrapperElem = jQuery(taxonAttributeWrapperSelector);
                
                taxonAttributeWrapperElem.append(data);
                if (data.length > 0) {
                    taxonAttributeWrapperElem.addClass("input_container");
                }
                
                bdrs.initDatePicker();
                jQuery(".acomplete").autocomplete({
                    source: function(request, callback) {
                        var params = {};
                        params.ident = bdrs.ident;
                        params.q = request.term;
                        var bits = this.element[0].id.split('_');
                        params.attribute = bits[bits.length-1];
            
                        jQuery.getJSON(bdrs.portalContextPath+'/webservice/attribute/searchValues.htm', params, function(data, textStatus) {
                            callback(data);
                        });
                    },
                    html: true,
                    minLength: 2,
                    delay: 300
                });
                
                taxonAttributeWrapperElem.ketchup();
            });
            
            // Update the Species Profile Table.
            // Clears old species profile template profile values and inserts
            // the new values.
            
            // Clear all new profile rows that do not have content.
            var addedRows = jQuery('input[name=new_profile]').parents('tr');
            var i;
            var deletionList = [];
            for(i=0; i<addedRows.length; i++) {
                var row = jQuery(addedRows[i]);
                var content = row.find("input[name ^= new_profile_content]");
                if(content.val().length === 0) {
                    row.remove();
                }
            }
            
            // Get the new taxon profile template.
            param = {};
            if(taxonPkElem.length > 0) {
                param.taxonPk = taxonPkElem.val();
            }
            param.groupPk = jQuery(groupPkSelector).val();
            param.index = jQuery(newProfileIndexSelector).val();
            jQuery.get(bdrs.portalContextPath+'/bdrs/admin/taxonomy/ajaxTaxonProfileTemplate.htm', param, function(data) {
                
                // Hide the table body to prevent any flickering as the dom is
                // modified.
                var tbody = jQuery(taxonProfileTableSelector).find("tbody");
                tbody.css({display:'none'});
                tbody.append(data);

                // First eliminate duplicate rows
                var row;
                var rows_to_append = [];
                var row_set = {};
                
                // Generic 'hashing' function to create a unique identifier for the row
                var getRowKey = function(row) {
                    var key = [];
                    key.push(row.find('input[name*=profile_type]').val());
                    key.push(row.find('input[name*=profile_header]').val());
                    key.push(row.find('input[name*=profile_description]').val());
                    return key.join('|');
                };
                
                // The following are rows with content so we never eliminate them no matter what.
                var all_rows = tbody.find("tr");
                var rows_with_content = all_rows.find('input[name*=_content_][value!=""]').parents("tr");
                for(i=0; i<rows_with_content.length; i++) {
                    row = jQuery(rows_with_content[i]);
                    rows_to_append.push(row);
                    row_set[getRowKey(row)] = row;
                }
                
                // For the remaining rows, if they are not in the row set, then
                // we add them. Otherwise we don't. 
                for(i=0; i<all_rows.length; i++) {
                    row = jQuery(all_rows[i]);
                    var key = getRowKey(row);
                    
                    if(row_set[key] === undefined) {
                        row_set[key] = row;
                        rows_to_append.push(row);
                    } 
                }
                
                // Rows are not merely added to the table because they would
                // not be sorted according to their weight.                   
                rows_to_append = rows_to_append.sort(function(a, b) {
                    var weight_a = parseInt(jQuery(a).find(".sort_weight").val(), 10);
                    var weight_b = parseInt(jQuery(b).find(".sort_weight").val(), 10);
                    
                    return weight_a - weight_b;
                });
                // Re-order the rows.
                tbody.empty();
                for(i=0;i<rows_to_append.length;i++) {
                    tbody.append(rows_to_append[i]);
                }
                tbody.css({display:''});
                
                // Reattach the drag and drop handlers
                bdrs.dnd.attachTableDnD(taxonProfileTableSelector);

                // Update the index to accomodate the new species profile template
                // the index needs to be the next available index.
                var profileIndexElem = jQuery(newProfileIndexSelector);
                var index = parseInt(profileIndexElem.val(), 10);
                var addedProfiles = jQuery('input[name=new_profile]');
                for(i=0; i<addedProfiles.length; i++) {
                    index = Math.max(index, jQuery(addedProfiles[i]).val());
                }
                profileIndexElem.val(index + 1);
            });
            
        },
        change: function(event, ui) {
            if(ui.item === null) {
                jQuery(groupPkSelector).val('');
                jQuery(groupAutocompleteSelector).val('');
                jQuery(taxonAttributeWrapperSelector).empty();
            }
        },
        html: true,
        minLength: 2,
        delay: 300
    });
    
    jQuery(function() {
        bdrs.dnd.attachTableDnD(taxonProfileTableSelector);
    });

    bdrs.taxonomy.addSecondaryGroupControls(secondaryGroupAutocompleteSelector, '.deleteSecondaryGroup a');

};

bdrs.taxonomy.addNewProfile = function(newProfileIndexSelector, profileTableSelector) {
    var profileIndexElem = jQuery(newProfileIndexSelector);
    var profileIndex = parseInt(profileIndexElem.val(), 10);
    profileIndexElem.val(profileIndex + 1);
    
    var params = {};
    params.index = profileIndex;
    
    jQuery.get(bdrs.portalContextPath+'/bdrs/admin/taxonomy/ajaxAddProfile.htm', params, function(data) {
        // add the new row
        var table = jQuery(profileTableSelector); 
        var row = jQuery(data);
        table.find("tbody").append(row);
        // add the dnd handler to the new row
        bdrs.dnd.attachTableDnD(profileTableSelector);
        bdrs.dnd.tableDnDDropHandler(table[0], row[0]); 
        row.ketchup();
        
        // Focus the row we just added, if the table is long it can be unclear if the add function did anything.
        jQuery(profileTableSelector).find('tr:last input[type=text]:first').focus();
    });
};

/**
 * Adds a row to the secondary group table.  It does this by selecting a row from a hidden prototype row and
 * inserting it into the secondary groups table.
 * @param tableSelector identifies the table to add the row to.
 * @param rowSelector identifies the div in which the prototype of the row to add is hidden.
 */
bdrs.taxonomy.addSecondaryGroup = function(tableSelector, rowSelector) {
    console.log(rowSelector);
    console.log(jQuery(rowSelector+' tr'));
    jQuery(tableSelector+' > tbody:last').append(jQuery(rowSelector+' tr').clone());
    jQuery(tableSelector+' > tbody:last').ketchup();


    var selectorPrefix = tableSelector + ' tr:last';
    jQuery(selectorPrefix+' .secondaryGroupSearch').focus();
    bdrs.taxonomy.addSecondaryGroupControls(selectorPrefix+' .secondaryGroupSearch', selectorPrefix + ' .deleteSecondaryGroup a');
};

/**
 * Adds autocomplete & delete actions to a row in the secondary groups table.
 * @param secondaryGroupAutocompleteSelector identifies the autocomplete field.
 * @param deleteSecondaryGroupSelector identifies the delete link.
 */
bdrs.taxonomy.addSecondaryGroupControls = function(secondaryGroupAutocompleteSelector, deleteSecondaryGroupSelector) {

    // Secondary Taxon Groups
    jQuery(secondaryGroupAutocompleteSelector).autocomplete({
        search: bdrs.taxonomy.taxonGroupSearcher.initSearch,
        source: bdrs.taxonomy.taxonGroupSearcher.searchTaxonGroups,
        select: function(event, ui) {
            var taxonGroup = ui.item.data;
            jQuery(event.target).next("input[type=text]").val(taxonGroup.id);
        }
    });
    if (deleteSecondaryGroupSelector !== undefined) {
        jQuery(deleteSecondaryGroupSelector).click(bdrs.taxonomy.deleteSecondaryGroup);
    }

};

bdrs.taxonomy.importALAProfile = function(guidListSelector, taxonGroupNameSelector, shortProfileSelector) { 
    if (jQuery(guidListSelector).val()) {
        if (!confirm("Are you sure? Existing entries imported from ALA will be replaced!")) {
            return false;
        }
    }
    if (!jQuery(taxonGroupNameSelector).val()) {
        if (!confirm("Are you sure you want to import these without a Taxon Group? They will end up in the Taxon Group called Life if you do this.")) {
            return false;
        }
    }
    
    var BLOCKER_TEXT = "The import is now running. ";
    var guidString = jQuery(guidListSelector).val(); 
    var guidArray = guidString ? guidString.split(",") : [];
    var taxonGroupName = jQuery(taxonGroupNameSelector).val();
    var shortProfile = jQuery(shortProfileSelector).is(':checked') ? 'true' : '';
    
    jQuery.blockUI({ message: '<h1 id="blockerMessage">' + BLOCKER_TEXT + '0 / '+ guidArray.length +'</h1>' });
    
    // current index
    var i = 0;
    var successCount = 0;
    var errorList = [];
    
    // Use closures and recursion to queue up our ajax requests.
    var ajaxImportFunc = function(guidArray, taxonGroupName, shortProfile) {
        jQuery.ajax({
            url: bdrs.portalContextPath + "/bdrs/admin/taxonomy/importNewProfiles.htm",
            type: "POST",
            data: {
                "guids": guidArray[i],
                "taxonGroup": taxonGroupName,
                "shortProfile": shortProfile
            },
            success: function(data) {
                if (data.errorList.length === 0) {
                  ++successCount;   
                } else {
                    // we process guids one at a time so if there is an error
                    // we know there will only be 1.
                    errorList.push(data.errorList[0]);
                }
            },
            error: function() {
                errorList.push("Error communicating with the server.");
            },
            complete: function() {
                jQuery('#blockerMessage').text(BLOCKER_TEXT + (i+1) +' / '+ guidArray.length);
                ++i;
                if (i < guidArray.length) {
                    ajaxImportFunc(guidArray, taxonGroupName, shortProfile);
                } else {
                    jQuery.unblockUI();
                    bdrs.message.set("Successfully imported " + successCount + " species.");
                    for (var j=0; j<errorList.length; ++j) {
                        bdrs.message.append(errorList[j]);  
                    }
                }
            }
        });
    };
    // kick off our first item...
    ajaxImportFunc(guidArray, taxonGroupName, shortProfile);
};

/**
 * Moves the supplied row to the top of it's enclosing table.
 * Used to allow a specific image to be configured as the profile default.
 * @param row a jQuery object wrapping the row to move to top.
 */
bdrs.taxonomy.moveRowToTop = function(row) {
    // Move the row to the top of the table.
    row.insertBefore(row.siblings().first());
    // Focus the first text field in the row so the user gets some indication of what has happened.
    row.find('input[type=text]').first().focus();
    // Manually invoke the drop handler so as to update the sort_weight for the rows in the table.
    bdrs.dnd.tableDnDDropHandler(row.parents("table:first")[0], row);

};

// -------------------------------------
// editTaxonGroup.jsp support functions
// -------------------------------------
/**
 * Initialises the table displaying taxon group members on the Edit Taxon Group page.
 * Also configures the functionality of the bulk update and search features that act on the table.
 * @param urlPrefix the prefix to use for ajax URLs (the web application context path).
 * @param groupId the id of the taxon group being edited.
 */
bdrs.taxonomy.initEditTaxonGroupMembers = function(urlPrefix, groupId) {
    // Initialize the grid
    var thumbnailFormatter = function(cellvalue, options, rowObject) {
        if (cellvalue != undefined && cellvalue != '') {
            return '<a href="'+urlPrefix+'/fieldguide/taxon.htm?id=' + rowObject.id +'">' +
                '<img class="max_size_img" src="'+urlPrefix+'/files/downloadByUUID.htm?uuid=' + cellvalue + '"/>' +
                '</a>';
        }
        return '';
    };

    var nameLinkFormatter = function(cellvalue, options, rowObject) {
        return '<a href="'+urlPrefix+'/fieldguide/taxon.htm?id=' + rowObject.id +'">' + cellvalue + '</a>';
    };

    var initParams = "?groupId="+groupId+"&primaryGroupOnly=true";


    var updateBulkActionButtonStatus = function() {

        var selectedTaxa = jQuery("#taxaList").getGridParam('selarrrow');
        var group = jQuery("#actionGroupId").val();

        if ((selectedTaxa !== undefined) && (selectedTaxa.length > 0) &&
            (group != undefined) && (parseInt(group) > 0)) {
            jQuery('#performGroupAction').removeAttr('disabled');
        }
        else {
            jQuery('#performGroupAction').attr('disabled', 'disabled');
        }
    };

    var reloadGrid = function() {
        jQuery("#taxaList").css('min-height', jQuery("#taxaList").css('height'));
        var searchResultsQuery = jQuery('#search_in_result').val();
        var queryString = "?groupId="+groupId+"&primaryGroupOnly=true&search_in_result=" + searchResultsQuery;
        jQuery("#taxaList").jqGrid().setGridParam({
            url:''+urlPrefix+'/fieldguide/listTaxa.htm' + queryString,
            page:1}).trigger("reloadGrid");
        updateBulkActionButtonStatus();
    };

    jQuery("#taxaList").jqGrid({
        url: urlPrefix+'/fieldguide/listTaxa.htm' + initParams,
        datatype: "json",
        mtype: "GET",
        colNames:['Scientific Name','Common Name', ''],
        colModel:[
            {name:'scientificName',index:'scientificName', width:150, classes:'scientificName', formatter:nameLinkFormatter},
            {name:'commonName',index:'commonName', width:150, formatter: nameLinkFormatter},
            {name:'thumbnail', index:'thumbnail', sortable:false, formatter:thumbnailFormatter, align:'center'}
        ],
        autowidth: true,
        jsonReader : { repeatitems: false },
        rowNum:50,
        rowList:[10,20,30,40,50,100],
        pager: '#pager2',
        sortname: 'scientificName',
        viewrecords: true,
        sortorder: "asc",
        width: '100%',
        height: "100%",
        multiselect: true,
        onSelectRow: updateBulkActionButtonStatus,
        onSelectAll: updateBulkActionButtonStatus
    });



    jQuery('#search_in_result_button').bind('click', reloadGrid);

    jQuery(".ui-jqgrid-bdiv").css('overflow-x', 'hidden');


    var action = function() {
        var url = urlPrefix+'/webservice/taxon/'+jQuery('#groupAction').val()+'.htm';
        var selectedTaxa = jQuery("#taxaList").getGridParam('selarrrow');
        var group = jQuery('#actionGroupId').val();
        jQuery.post(url, {taxonGroupId:group, taxonId: selectedTaxa}, function(data) {
            reloadGrid();
        });


    };

    jQuery('#actionGroup').autocomplete({
        search: bdrs.taxonomy.taxonGroupSearcher.initSearch,
        source: bdrs.taxonomy.taxonGroupSearcher.searchTaxonGroups,
        select: function(event, ui) {
            var taxonGroup = ui.item.data;
            jQuery(event.target).next("input[type=text]").val(taxonGroup.id);
            updateBulkActionButtonStatus();
        }
    });
    jQuery('#performGroupAction').click(action);
    jQuery('#actionGroup').change(updateBulkActionButtonStatus);

    updateBulkActionButtonStatus();
}
