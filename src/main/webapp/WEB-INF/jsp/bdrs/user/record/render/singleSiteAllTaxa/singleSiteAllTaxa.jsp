<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<jsp:useBean id="survey" scope="request" type="au.com.gaiaresources.bdrs.model.survey.Survey" />

<%@page import="au.com.gaiaresources.bdrs.model.taxa.Attribute"%>
<%@page import="au.com.gaiaresources.bdrs.model.taxa.AttributeScope"%>

<%-- Access the facade to retrieve the preference information --%>
<jsp:useBean id="bdrsPluginFacade" scope="request" type="au.com.gaiaresources.bdrs.servlet.BdrsPluginFacade"></jsp:useBean>
<c:set var="showScientificName" value="<%= bdrsPluginFacade.getPreferenceBooleanValue(\"taxon.showScientificName\") %>" />

<h1><c:out value="${survey.name}"/></h1>
<c:if test="${censusMethod != null}">
    <!-- using censusmethod description here in case we want to display no text / more indepth text -->
    <p><cw:validateHtml html="${censusMethod.description}"></cw:validateHtml></p>
</c:if>

<c:if test="${displayMap}">
    <tiles:insertDefinition name="recordEntryMap">
        <tiles:putAttribute name="survey" value="${survey}"/>
        <tiles:putAttribute name="censusMethod" value="${censusMethod}"/>
    </tiles:insertDefinition>
</c:if>
    
    <c:if test="${ not preview && recordWebFormContext.editable }">
        <form method="POST" action="${portalContextPath}/bdrs/user/singleSiteAllTaxa.htm" enctype="multipart/form-data">
    </c:if>
    <input type="hidden" name="surveyId" value="${survey.id}"/>
    
    <%-- the record form header contains the unlock form icon --%>
    <tiles:insertDefinition name="recordFormHeader">
        <tiles:putAttribute name="recordWebFormContext" value="${recordWebFormContext}" />
    </tiles:insertDefinition>
    
    <div id="tableContainer">
    
    <div id="surveyScopedFieldsContainer">
	    <%-- Just a tile to help avoid DRY between SSMT and SSAT forms --%>
	    <tiles:insertDefinition name="singleSiteSurveyScopeFields">
	    </tiles:insertDefinition>
	    <%-- Allows you to make this a clearing div at the end of the container --%>
	    <div id="surveyScopedFieldsContainerEnd"></div>
    </div>
    
    <div id="sightingsContainer">
        <!-- Add sightings description text -->
        <cw:getContent key="user/singleSiteMultiTaxaTable" />
        
        <c:if test="${ recordWebFormContext.editable }">
            <div id="add_sighting_panel" class="buttonpanel textright">
                <a id="maximiseLink" class="text-left" href="javascript:bdrs.util.maximise('#maximiseLink', '#sightingsContainer', 'Enlarge Table', 'Shrink Table')">Enlarge Table</a>
                <input type="hidden" id="sighting_index" name="sightingIndex" value="${fn:length(recordWebFormContext.namedCollections['recordFieldCollectionList'])}"/>
                <input class="form_action" type="button" value="Add Sighting" onclick="bdrs.contribute.singleSiteMultiTaxa.addSighting('#sighting_index', '[name=surveyId]', '#sightingTable tbody', false, false, ${showScientificName});"/>
            </div>
        </c:if>
        
        <table id="sightingTable" class="datatable">
            <thead>
               <tr>
                   <c:forEach items="${ recordWebFormContext.namedFormFields['sightingRowFormFieldList'] }" var="sightingRowFormField">
                           <jsp:useBean id="sightingRowFormField" type="au.com.gaiaresources.bdrs.controller.attribute.formfield.AbstractRecordFormField" />
                           <c:if test="<%= sightingRowFormField.isPropertyFormField() %>">
                                <c:choose>
                                    <c:when test="${ not sightingRowFormField.hidden }">
                                        <c:if test="${sightingRowFormField.scope == 'RECORD'}">
                                            <th class="st_header_${ sightingRowFormField.name }">
                                                <c:out value="${ sightingRowFormField.description }" />
                                            </th>
                                        </c:if>
                                    </c:when>
                                </c:choose>
                            </c:if> 
                            <c:if test="<%= sightingRowFormField.isAttributeFormField() %>">
                                <c:if test="${ sightingRowFormField.attribute.scope == 'RECORD' || sightingRowFormField.attribute.scope == 'RECORD_MODERATION'}">
                                    <th class="st_header_${ sightingRowFormField.name }">
                                    <c:out value="${ sightingRowFormField.attribute.description }" />
                                    </th>
                                </c:if>
                            </c:if>
                        
                   </c:forEach>
                   <c:if test="${ not preview and recordWebFormContext.editable and not recordWebFormContext.moderateOnly }">
                       <th>Delete</th>
                   </c:if>
               </tr>
            </thead>
            <tbody>
                <%-- Insert existing records here. --%>
                
                <c:forEach items="${recordWebFormContext.namedCollections['recordFieldCollectionList']}" var="recordFormFieldCollection">
                    <tiles:insertDefinition name="singleSiteMultiTaxaRow">
                        <tiles:putAttribute name="recordFormFieldCollection" value="${recordFormFieldCollection}"/>
                        <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                        <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                        <tiles:putAttribute name="editEnabled" value="${ recordWebFormContext.editable }" />
                        <tiles:putAttribute name="isModerationOnly" value="${ recordWebFormContext.moderateOnly }" />
                        <tiles:putAttribute name="speciesEditable" value="<%= false %>" />
                    </tiles:insertDefinition>
                </c:forEach>
            </tbody>
        </table>
    </div>
    
    </div>
    
<%-- the record form footer contains the 'form' close tag --%>
<tiles:insertDefinition name="recordFormFooter">
    <tiles:putAttribute name="recordWebFormContext" value="${recordWebFormContext}" />
</tiles:insertDefinition>

<noscript>
    <tiles:insertDefinition name="noscriptMessage"></tiles:insertDefinition>
</noscript>

<script type="text/javascript">
    jQuery(function() {
        jQuery("#script_content").removeClass("hidden");
    });
</script>

<script type="text/javascript">
    jQuery(window).load(function() {
        /**
         * Prepopulate fields
         */
        bdrs.form.prepopulate();
    });
</script>
