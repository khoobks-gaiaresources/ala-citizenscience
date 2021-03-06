<!-- list the user groups currently in the database  -->
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>

<h1>Edit Map Layer</h1>

<p>This is where you may create and edit map layers. Once you have created your map layers, assign them to a map using the <a href="${portalContextPath}/bdrs/admin/map/listing.htm">map editing interface</a></p>

<tiles:insertDefinition name="geoMapLayerGrid">
       <tiles:putAttribute name="widgetId" value="mapLayerList"/>
       <tiles:putAttribute name="multiselect" value="false"/>
       <tiles:putAttribute name="scrollbars" value="false" />
       <tiles:putAttribute name="showActions" value="true" />
       <tiles:putAttribute name="editUrl" value="${portalContextPath}/bdrs/admin/mapLayer/edit.htm" />
       <tiles:putAttribute name="deleteUrl" value="${portalContextPath}/bdrs/admin/mapLayer/delete.htm" />
</tiles:insertDefinition>

<div class="buttonpanel textright">
    <input class="form_action" type="button" value="Add Map Layer" onclick="window.location = '${portalContextPath}/bdrs/admin/mapLayer/edit.htm';"/>
</div>