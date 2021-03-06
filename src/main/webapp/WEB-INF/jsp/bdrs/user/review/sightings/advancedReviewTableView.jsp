<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="math" uri="/WEB-INF/math.tld" %>

<tiles:useAttribute name="sortBy" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="sortOrder" classname="java.lang.String" ignore="true"/>
<tiles:useAttribute name="resultsPerPage" classname="java.lang.Integer" ignore="true"/>
<tiles:useAttribute name="pageCount" classname="java.lang.Long" ignore="true"/>
<tiles:useAttribute name="pageNumber" classname="java.lang.Long" ignore="true"/>
<tiles:useAttribute name="resultsType" classname="java.lang.String" ignore="true"/>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="viewStyle" value="<%= bdrsPluginFacade.getPreferenceValue(\"review.advancedReview.viewStyle\") %>" />
<%-- The view style will default to TABLE in the event of a missing or invalid portal preference value --%>

<div class="alaSightingsTableViewContent">
    <div class="sortPanel">
        <%-- The results messages will contain any messages about the results on the page
             Currently, this is only used in the Location Review for a message 
             about map selection and a link to clear the map --%>
        <div id="resultsMessages" class="left"></div>
        <div class="${viewStyle == 'DIV' ? 'left' : 'right'}">
            <label for="resultsPerPage">Results per page</label>
            <select id="resultsPerPage" name="resultsPerPage">
                <option <c:if test="${resultsPerPage == 10}">selected="selected"</c:if>>10</option>
                <option <c:if test="${resultsPerPage == 20}">selected="selected"</c:if>>20</option>
                <option <c:if test="${resultsPerPage == 50}">selected="selected"</c:if>>50</option>
                <option <c:if test="${resultsPerPage == 100}">selected="selected"</c:if>>100</option>
            </select>
        </div>

        <c:choose>
            <c:when test="${viewStyle == 'DIV'}">
                <div class="right">
                    <label for="sortOrder">Sort Order</label>
                    <select id="sortOrder" name="sortOrder">
                        <option value="ASC"
                            <c:if test="${ 'ASC' == sortOrder }">
                                selected="selected"
                            </c:if>
                        >
                            ascending
                        </option>
                        <option value="DESC"
                            <c:if test="${ 'DESC' == sortOrder }">
                                selected="selected"
                            </c:if>
                        >
                            descending
                        </option>
                    </select>
                </div>
                
                <div class="right">
                    <label for="sortBy">Sort By</label>
                    <select id="sortBy" name="sortBy">
                        <script>
                            jQuery(document).ready(function() {
                            	var selectElem = jQuery("#sortBy");
                            	$(tableColumns).each(function(i, column) {
                            		var opt = jQuery("<option/>");
                            		opt.val(column.sortName);
                            		if ((column.sortName == 'record.when' && 
                            		     'when' == ${sortBy}) ||
                            		     column.sortName == ${sortBy}) {
                            			opt.attr("selected", true);
                            		}
                            		selectElem.append(opt);
                            	});
                            });
                        </script>
                    </select>
                </div>
            </c:when>
            <c:otherwise>
                <%-- default to table style view --%>
                <input id="sortBy" type="hidden" name="sortBy" value="${sortBy}" />
                <input id="sortOrder" type="hidden" name="sortOrder" value="${sortOrder}" />
            </c:otherwise>
        </c:choose>
        
        <div class="clear"></div>
    </div>
    
    <c:choose>
        <c:when test="${viewStyle == 'DIV'}">
            <span id="alaSightingsTable" class="alaSightingsTable"></span>
        </c:when>
        <c:otherwise>
            <table id="alaSightingsTable" class="alaSightingsTable">
                <thead id="alaSightingsTableHeader" class="columnBanner">
                    <tr id="headerRow">
                        <script>
                            jQuery(document).ready(function() {
                            	var header = jQuery("#headerRow");
                            	$(tableColumns).each(function(i, column) {
                            		var col = jQuery("<td/>");
                            		col.addClass("sortBy("+column.sortName+")");
                            		col.addClass(column.tdClass);
                            		var colDiv = jQuery("<div/>");
                            		colDiv.attr("title", column.tooltip);
                            		colDiv.addClass(column.divClass);
                            		colDiv.html(column.title);
                            		col.append(colDiv);
                            		jQuery("#lastColumn").before(col);
                            	});
                            });
                        </script>
                        
                        <td id="lastColumn" style="width:auto">
                                <sec:authorize ifAnyGranted="ROLE_ADMIN,ROLE_SUPERVISOR,ROLE_POWER_USER,ROLE_USER">
                            <input id="bulkSelectCheckbox" title="Select/deselect all" type="checkbox"  />
                        </sec:authorize>
                            </td>
                    </tr>
                </thead>
                <tbody>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
    
    <sec:authorize ifAnyGranted="ROLE_USER, ROLE_POWER_USER, ROLE_SUPERVISOR, ROLE_ADMIN">
        <c:if test="${resultsType == 'record' }">
            <script type="text/javascript">
                function submitReclassify() {
                    // Parse input
                    var selectedRecordIds = [];
                    jQuery('.recordIdCheckbox:checked').each(function(index, element) {
                        selectedRecordIds.push(jQuery(element).val());
                    });
                    var nbRecordsSelected = selectedRecordIds.length;
                    var speciesId = jQuery("input#species_id").val();
                    var speciesName = jQuery("input#survey_species_search").val();
                    var massReclassifySelected = jQuery('input#massReclassifyCB').is(':checked');
                    //check input validity
                    if (!speciesId) {
                        alert("No species selected for reclassification.");
                        return false;
                    }
                    if (nbRecordsSelected === 0 && !massReclassifySelected){
                        alert("Please select records to be reclassified or select the Reclassify All option.");
                        return false;
                    }
                    // Everything seems OK.
                    var isMassReclassify = (nbRecordsSelected === 0 && massReclassifySelected);
                    var size = nbRecordsSelected;
                    if (isMassReclassify) {
                        // to get the number of records we need to read the
                        // count label at the top of the form
                        var text = jQuery("span[id='count']").text().trim();
                        var regex = /^.*(?:[0-9]+)/; // grab the first integer in the text
                        var result = regex.exec(text);
                        size = result[0];  // if failed to find a number, size = undefined (which is ok) 
                    }
                    var confirmMsg = "You're about to reclassify " + size + " record" + (size > 1 ? "s" : "") + " into:\n" +
                                     speciesName + ".\n Are you sure?"; 
                    if (!confirm(confirmMsg)) {
                        return false;
                    }
                    bdrs.advancedReview.submitReclassify(selectedRecordIds, speciesId, isMassReclassify);
                    return true;
                }
            </script>
            <div class="bulkActionContainer">
                <div id="reclassifyInputPanel" class="reclassify" style="display:none;">      
                    <table class="form_table">
                        <tr >
                            <th>Species</th>
                            <td style="padding-bottom: 0px">     
                                <input id="survey_species_search" type="text" name="survey_species_search" class="ui-autocomplete-input" autocomplete="off" role="textbox" aria-autocomplete="list" aria-haspopup="true"/>
                                <input type="text" class="speciesIdInput" id="species_id" name="speciesId"/>
                                <script type="text/javascript">
                                    jQuery(function() {
                                        bdrs.contribute.initSpeciesAttributeAutocomplete('#survey_species_search', '#species_id', 1, true);
                                    });
                                </script>
                            </td>
                            <td>
                                <input class="form_action" type="button" name="reclassify" value="Reclassify" onclick="submitReclassify();"/>
                            </td>
                        </tr>
                        <tr>
                            <td></td>
                            <td id="massReclassifyPanel">
                                <input id="massReclassifyCB" type="checkbox"/>
                                <label for="massReclassifyCB">Reclassify All</label>
                            </td>
                        </tr>
                    </table>
                </div>
                <div class="right">
	                <span>Apply action to selected records: </span>
	                <a title="Delete the selected records" href="javascript:void(0)" onclick="bdrs.advancedReview.bulkDelete()">Delete</a>
	                <sec:authorize ifAnyGranted="ROLE_SUPERVISOR, ROLE_ADMIN">
	                   |<a title="Hold the selected records" href="javascript:void(0)" onclick="bdrs.advancedReview.bulkModerate(true)">Hold</a>
	                   |<a title="Release the selected records" href="javascript:void(0)" onclick="bdrs.advancedReview.bulkModerate(false)">Release</a>
                       |<a title="Reclassify the selected records" href="javascript:void(0)" onclick="toggleReclassifyPanel();">Reclassify</a>
                       <script type="text/javascript">
                            function toggleReclassifyPanel(){
                                var elt = jQuery("div#reclassifyInputPanel");
                                if (elt) {
                                    var newVisibility = elt.css('display') === 'none' ? 'block' : 'none';
                                    elt.css('display', newVisibility);
                                }
                            }     
                       </script>
	                </sec:authorize>
	                <c:if test="${viewStyle == 'DIV'}">
	                    <input id="bulkSelectCheckbox" title="Select/deselect all" type="checkbox" />
	                </c:if>
                </div>
            </div>
        </c:if>
    </sec:authorize>
    
    <div class="clear"></div>
</div>

<%-- may want to make paginationRange and maxPaginationItems portal prefs in the future --%>
<%-- default number of pages to look for on either side of current page --%>
<c:set var="paginationRange" value="4" />
<%-- maximum number of pages to show including the current page --%>
<c:set var="maxPaginationItems" value="9" />
<%-- min page number to show --%>
<c:set var="minPage" value="${math:max(pageNumber-paginationRange,1)}" />
<%-- max page number to show --%>
<c:set var="maxPage" value="${math:min(minPage+maxPaginationItems-1, pageCount)}" />

<div class="textcenter">
   <div id="searchNavBar">
     <input type="hidden" value="${ pageNumber }" name="pageNumber" id="pageNumber"/>
     <ul>
     <c:choose>
        <c:when test="${ pageNumber == 1 }">
            <li id="prevPage">&#171;&nbsp;Previous</li>
        </c:when>
        <c:otherwise>
            <li id="prevPage"><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${pageNumber-1});">&#171;&nbsp;Previous</a></li>
        </c:otherwise>
     </c:choose>
     <c:forEach var="i" begin="${minPage}" end="${maxPage}" step="1" varStatus ="status">
         <c:choose>
            <c:when test="${ pageNumber == i }">
                <li class="currentPage"><c:out value="${i}"/></li>
            </c:when>
            <c:otherwise>
                <li><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${i});"><c:out value="${i}"/></a></li>
            </c:otherwise>
         </c:choose>
     </c:forEach>
     <c:choose>
        <c:when test="${ pageNumber == pageCount }">
            <li id="nextPage">Next&nbsp;&#187;</li>
        </c:when>
        <c:otherwise>
            <li id="nextPage"><a class="pageLink" href="javascript:bdrs.advancedReview.pageSelected(${pageNumber+1});">Next&nbsp;&#187;</a></li>
        </c:otherwise>
     </c:choose>
     </ul>
   </div>
</div>

<script type="text/javascript">
    jQuery(function() {
        bdrs.advancedReview.initTableView('#facetForm',  
            '#alaSightingsTable', 'input[name=sortOrder]', 'input[name=sortBy]', 
            'select[name=resultsPerPage]', "${viewStyle != null ? viewStyle : 'null'}", "#bulkSelectCheckbox");
    });
</script>
