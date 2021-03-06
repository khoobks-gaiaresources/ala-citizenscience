<!-- list the user groups currently in the database  -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Manage Maps</h1>

<p>
	Create, edit and delete maps from this interface. You will have previously needed 
    to create map layers to assign to maps using the the 
    <a href="${portalContextPath}/bdrs/admin/mapLayer/listing.htm">map layer interface.</a>
    Note that the map will not be visible until Publish is set to 'Yes'.
</p>

<tiles:insertDefinition name="geoMapGrid">
       <tiles:putAttribute name="widgetId" value="mapList"/>
       <tiles:putAttribute name="multiselect" value="false"/>
       <tiles:putAttribute name="scrollbars" value="false" />
       <tiles:putAttribute name="showActions" value="true" />
       <tiles:putAttribute name="editUrl" value="${portalContextPath}/bdrs/admin/map/edit.htm" />
       <tiles:putAttribute name="deleteUrl" value="${portalContextPath}/bdrs/admin/map/delete.htm" />
</tiles:insertDefinition>

<div class="buttonpanel textright">
    <input class="form_action" type="button" value="Add Map" onclick="window.location = '${portalContextPath}/bdrs/admin/map/edit.htm';"/>
</div>