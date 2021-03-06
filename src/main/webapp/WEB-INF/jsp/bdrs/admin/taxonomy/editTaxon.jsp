<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<%@page import="au.com.gaiaresources.bdrs.model.taxa.TaxonRank"%>

<jsp:useBean id="taxon" type="au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies" scope="request"/>
<jsp:useBean id="taxonProfileList" type="java.util.List" scope="request"/>
<jsp:useBean id="formFieldList" type="java.util.List" scope="request"/>

<c:choose>
    <c:when test="${ taxon.id == null }">
        <h1>Add a New Taxon</h1>
    </c:when>
    <c:otherwise>
        <h1>Edit an Existing Taxon</h1>
    </c:otherwise>
</c:choose>

<cw:getContent key="admin/taxonomy/editTaxonomy" />
<form id="speciesForm" method="POST" action="${portalContextPath}/bdrs/admin/taxonomy/edit.htm" enctype="multipart/form-data">
    <div class="textright buttonpanel">
        <input type="submit" class="form_action" value="Save"/>
    </div>

    <c:if test="${ taxon.id != null }">
       <input id="taxonPk" type="hidden" name="taxonPk" value="${ taxon.id }"/>
       <!-- hidden field used for adding census method attribute rows -->
       <input id="id_species_id" type="hidden" name="speciesId" value="${ taxon.id }"/>
   </c:if>
   <div class="input_container" style="float:left; width: 45%">
        <table class="form_table taxon_edit_form_table">
            <tbody>
                <tr>
                    <th><label for="scientificName">Scientific Name</label></th>
                    <td><input id="scientificName" class="validate(required)" type="text" name="scientificName" value="${ taxon.scientificName }"/></td>
                </tr>
                <tr>
                    <th><label for="commonName">Common Name</label></th>
                    <td><input id="commonName" class="validate(required)" type="text" name="commonName" value="${ taxon.commonName }"/></td>
                </tr>
                <tr>
                    <th><label for="rank">Rank</label></th>
                    <td>
                        <select id="rank" name="taxonRank">
                            <c:forEach var="rankEnum" items="<%= TaxonRank.values() %>">
                                <jsp:useBean id="rankEnum" type="au.com.gaiaresources.bdrs.model.taxa.TaxonRank"/>
                                <option value="<%= rankEnum.toString() %>"
                                    <c:if test="<%= (taxon.getTaxonRank() != null && rankEnum.equals(taxon.getTaxonRank())) || (taxon.getTaxonRank() == null && TaxonRank.SPECIES.equals(rankEnum)) %>">
                                        selected="selected"
                                    </c:if>
                                >
                                    <%= rankEnum.getIdentifier() %>
                                </option>
                            </c:forEach>
                        </select>
                    </td>
                </tr>
                <tr>
                    <th><label for="parent">Parent</label></th>
                    <td>
                        <input id="parent" type="text" name="parent" value="${ taxon.parent.scientificName }"/>
                        <input id="parentPk" type="text" name="parentPk" value="${ taxon.parent.id }" style="visibility:hidden;width:1px;padding:2px 0 0 0;margin:0 0 0 -4px;border-width:0;"/>
                    </td>
                </tr>

                <tr>
                    <th><label for="author">Author</label></th>
                    <td><input id="author" class="" type="text" name="author" value="${ taxon.author }"/></td>
                </tr>
                <tr>
                    <th><label for="year">Year</label></th>
                    <td><input id="year" class="" type="text" name="year" value="${ taxon.year }"/></td>
                </tr>
                <tr>
                    <th><label for="guid">Guid</label></th>
                    <td><input class="" type="text" name="guid" id="guid" value="${ taxon.sourceId }"/></td>
                </tr>

            </tbody>
          </table>
       </div>
       <div class="input_container" style="margin-left: 50%">
          <table class="form_table taxon_edit_form_table">
             <tbody>
               <tr>
                 <th><label for="taxonGroup">Primary Group</label></th>
                 <td>
                     <input id="taxonGroup" class="" type="text" name="taxonGroup" value="${ taxon.taxonGroup.name }"/>
                     <input id="taxonGroupPk" class="hiddenTextField validate(required)" type="text" name="taxonGroupPk" value="${ taxon.taxonGroup.id }"/>
                 </td>
                </tr>
                <tr>
                    <th>Secondary Groups</th>
                    <td>
                        <table id="secondaryGroups">
                            <tbody>
                                <c:forEach var="group" items="${taxon.secondaryGroups}">
                                   <tiles:insertDefinition name="secondaryTaxonGroupRow">
                                       <tiles:putAttribute name="group" value="${group}"/>
                                   </tiles:insertDefinition>
                                </c:forEach>

                            </tbody>
                        </table>
                    </td>
                </tr>
               <tr id="addSecondaryGroup">
                   <th></th>
                   <td>
                       <input type="button" class="form_action" style="margin: 2px 10px 10px 10px;" value="Add" onclick="bdrs.taxonomy.addSecondaryGroup('#secondaryGroups', '#secondaryTaxonGroupRow');"/>
                   </td>
               </tr>
            </tbody>
        </table>
    </div>

    <h3 style="clear:both;">Taxon Profile</h3>
    <p>
       The taxon profile provides additional data about this taxon such as
       distinctive markings, identifying characteristics, habitat and biology. 
    </p>
    <div id="editTaxonomyContainer" class="input_container">
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#editTaxonomyContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
           <input type="button" class="form_action" value="Retrieve Profile from ALA" onclick="importALAProfile();"/>
           <input type="button" class="form_action" value="Add Profile" onclick="bdrs.taxonomy.addNewProfile('#newProfileIndex', '#taxonProfileTable');"/>
        </div>
        <div class="scrollable">
            <table id="taxonProfileTable" class="datatable textcenter">
                <thead>
                    <tr>
                        <th>&nbsp;</th>
                        <th>Type</th>
                        <th>Database Name</th>
                        <th>Title</th>
                        <th>Content</th>
                        <th>Delete</th>
                    </tr>
                </thead>
                <tbody>
                    <tiles:insertDefinition name="profileTableBody">
                        <tiles:putAttribute name="taxonProfileList" value="${ taxonProfileList }"/>
                        <tiles:putAttribute name="newProfileIndex" value="${ newProfileIndex }"/>
                    </tiles:insertDefinition>
                </tbody>
            </table>
        </div>
       <input id="newProfileIndex" type="hidden" value="<%= taxonProfileList.size() + 1 %>"/>
        <div class="textright buttonpanel">
            <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#editTaxonomyContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
            <input type="button" class="form_action" value="Retrieve Profile from ALA" onclick="importALAProfile();"/>
            <input type="button" class="form_action" value="Add Profile" onclick="bdrs.taxonomy.addNewProfile('#newProfileIndex', '#taxonProfileTable');"/>
        </div>
    </div>
    <h3>Group Attributes</h3>
    <p>
        Group attributes are the custom attributes of this taxon as specified
        by this taxon group.
    </p>
    <div id="taxonAttributeTable"
        <c:if test="<%= formFieldList.size() > 0 %>">class="input_container"</c:if>
    >
        <tiles:insertDefinition name="taxonAttributeTable">
            <tiles:putAttribute name="formFieldList" value="${ formFieldList }"/>
        </tiles:insertDefinition>
    </div>
    
    <div class="textright buttonpanel">
       <input type="submit" class="form_action" value="Save"/>
    </div>
</form>

<div id="secondaryTaxonGroupRow">
    <table>
        <tiles:insertDefinition name="secondaryTaxonGroupRow"/>
    </table>
</div>

<jsp:include page="/WEB-INF/jsp/bdrs/dialog/attachFileDialog.jsp"/>
    
<jsp:include page="/WEB-INF/jsp/bdrs/dialog/htmlEditorDialog.jsp"/>
    

<cw:speciesProfileType/>
<script type="text/javascript">
    jQuery(function() {
        bdrs.taxonomy.initEditTaxon("#parent","#parentPk", 
                                    "#taxonGroup", "#taxonGroupPk",
                                    "#taxonPk", "#taxonAttributeTable", 
                                    "#taxonProfileTable", "#newProfileIndex",
                                    ".secondaryGroupSearch");

        // Add the preferred image selector to the attach file dialog
        var tbody = jQuery('#attachFileDialog').find('form table tbody');
        tbody.first().append(jQuery('<tr>\
                <th>Make this the preferred profile image</th>\
                <td>\
                    <input type="checkbox" name="addNewPreferred" id="addNewPreferred"/>\
                </td>\
            </tr>'));
        tbody.last().append(jQuery('<tr>\
                <td class="formLabel">Make this the preferred profile image</td>\
                <td><input type="checkbox" name="selectPreferred" id="selectPreferred"/></td>\
            </tr>'));
        
        // Attach focus event handlers to the text fields in the Content column of the profile table.
        // New rows in the table have different values for the name attribute, hence the requirement for
        // two event handlers.
        jQuery('#taxonProfileTable').delegate('input[name^=profile_content_]', 'focus', 'profile_type_', contentFocusHandler);
        jQuery('#taxonProfileTable').delegate('input[name^=new_profile_content_]', 'focus', 'new_profile_type_', contentFocusHandler);

        jQuery('#secondaryTaxonGroupRow').hide();
    });


    var importALAProfile = function() {
        var taxonPk = jQuery('#taxonPk').val();
        var guid = jQuery('#guid').val();

        if (!guid && !taxonPk) {
            alert("You must enter a GUID or be editing an existing species in order to use this feature.");
        } else {
            var answer = confirm("Are you sure? Existing entries imported from ALA will be replaced!")
            if (answer) {
                var url = '${portalContextPath}/bdrs/admin/taxonomy/import.htm?pk='
                    + (taxonPk ? taxonPk : '') +
                    (guid ? '&guid='+guid : '');
                window.document.location = url;
            }
        }
    };

    var showHtmlEditor = function(textField) {
        bdrs.attribute.showHtmlEditor($('#htmlEditorDialog'), $('#markItUp')[0], textField);
    };

    /**
     * Responds to focus events fired by the text field in the Content column of the Species profile table.
     * @param event event.data must contain the prefix used by the name attribute of the select element in the
     * same row as the text field that fired this event.
     */
    var contentFocusHandler = function(event) {

        var type = jQuery(this).parents('tr:first').find('select[name^='+event.data+']').attr("value");

        if (bdrs.taxonomy.speciesProfileType.isTextType(type)) {
            showHtmlEditor(this);
        }
        else if (bdrs.taxonomy.speciesProfileType.isFileType(type) || bdrs.taxonomy.speciesProfileType.isImageType(type)) {
            showFileSelector(this);
        }
    };
</script>
