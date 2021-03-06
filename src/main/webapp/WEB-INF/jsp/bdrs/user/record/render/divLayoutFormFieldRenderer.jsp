<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/cw.tld" prefix="cw" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<%@page import="au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem"%>
<%@page import="au.com.gaiaresources.bdrs.servlet.RequestContextHolder"%>
<%@page import="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField"%>
<%@page import="au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordAttributeFormField"%>
<%@page import="au.com.gaiaresources.bdrs.model.taxa.AttributeType"%>

<tiles:useAttribute name="formField" classname="au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField"/>
<tiles:useAttribute name="locations" classname="java.util.Set" ignore="true"/>
<tiles:useAttribute name="errorMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="valueMap" classname="java.util.Map" ignore="true"/>
<tiles:useAttribute name="editEnabled" ignore="true"/>
<tiles:useAttribute name="isModerationOnly" ignore="true"/>

<c:set var="form_field_name" value="${ fn:replace(formField.name, \" \", \"_\") }" />

     <c:choose>
         <c:when test="<%= formField.isModerationFormField() %>">
             <c:if test="${editEnabled}">
                 <c:set var="editEnabled" value="<%= RequestContextHolder.getContext().getUser().isModerator() || formField.isPropertyFormField() %>"></c:set>
             </c:if>
         </c:when>
         <c:otherwise>
             <c:if test="${editEnabled}">
                 <c:set var="editEnabled" value="${not isModerationOnly}"></c:set>
             </c:if>
         </c:otherwise>
     </c:choose>

<c:choose>
    <c:when test="<%= formField.isDisplayFormField() && !formField.isPropertyFormField() %>">
        <c:choose>
            <c:when test="<%= RecordAttributeFormField.class.isAssignableFrom(formField.getClass()) && 
            ((RecordAttributeFormField)formField).getAttribute().getType() == AttributeType.HTML_RAW %>">
                <%= ((RecordAttributeFormField)formField).getDescription() %>
            </c:when>
            <c:otherwise>
	            <%-- Fields for display only, such as, comments, horizontal rules, HTML, etc --%>
                <c:set var="isVisible" value="true"></c:set>
                <div id="ff_${ form_field_name }" class="form_field_pair">
                    <div>
                        <tiles:insertDefinition name="attributeRenderer">
                            <tiles:putAttribute name="formField" value="${formField}"/>
                            <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                        </tiles:insertDefinition>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </c:when>
  <c:otherwise>
  <c:choose>
    <c:when test="<%= formField.isPropertyFormField() %>">
       <%-- Special Handling for Lat and Lng (Position) --%>
       <c:choose>
          <c:when test="${ 'Point' == formField.propertyName }">
              <c:set var="readOnly" value="<%= ((RecordPropertyFormField)formField).getSurvey().isPredefinedLocationsOnly() %>"/>
              <c:set var="selectedSrid" value="<%= ((RecordPropertyFormField)formField).getRecord() != null && ((RecordPropertyFormField)formField).getRecord().getGeometry() != null ? ((RecordPropertyFormField)formField).getRecord().getGeometry().getSRID() : 0 %>" />
              <c:set var="xCoord" value="<%= ((RecordPropertyFormField)formField).getRecord() != null ? ((RecordPropertyFormField)formField).getRecord().getLongitude() : null %>" />
              <c:set var="yCoord" value="<%= ((RecordPropertyFormField)formField).getRecord() != null ? ((RecordPropertyFormField)formField).getRecord().getLatitude() : null %>" />
              <c:set var="editEnabled" value="${ editEnabled }"/>
              <c:set var="required" value="${ formField.required }" />
              <%
                  BdrsCoordReferenceSystem crs = ((RecordPropertyFormField)formField).getCrs();
                  Integer selectedSrid = (Integer)pageContext.getAttribute("selectedSrid");
                  pageContext.setAttribute("crsFieldRequired", crs.isZoneRequired() || (selectedSrid != 0 && crs.getSrid() != selectedSrid.intValue()));
                  pageContext.setAttribute("selectedCrs", selectedSrid != 0 ? BdrsCoordReferenceSystem.getBySRID(selectedSrid) : crs);
              %>
              <%-- At this point we know that the FormField is a RecordPropertyFormField --%>
                <c:if test="${not formField.hidden}">
                    <c:if test="${ crsFieldRequired }">
                        <div id="ff_${ form_field_name }" class="form_field_pair">
                            <div class="form_field_label"><label for="srid">Zone</label></div>
                            <div class="form_field_input">
                                <tiles:insertDefinition name="coordFormField">
                                    <tiles:putAttribute name="crs" value="${ formField.crs }"/>
                                    <tiles:putAttribute name="isZone" value="true"/>
                                    <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                                    <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                                    <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
                                    <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
                                    <tiles:putAttribute name="xCoord" value="${ xCoord }" />
                                    <tiles:putAttribute name="yCoord" value="${ yCoord }" />
                                    <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                                    <tiles:putAttribute name="required" value="${ formField.required }" />
                                </tiles:insertDefinition>
                            </div>
                        </div>
                    </c:if>

                  <%
                    // declared above
                    //BdrsCoordReferenceSystem crs = ((RecordPropertyFormField)formField).getCrs();
                    if (crs.isXfirst()) {
                       pageContext.setAttribute("coordFieldId", "ff_longitude");
                       pageContext.setAttribute("coordFieldName", crs.getXname());
                    } else {
                       pageContext.setAttribute("coordFieldId", "ff_latitude");
                       pageContext.setAttribute("coordFieldName", crs.getYname());
                    }
                  %>

                  <div id="${ coordFieldId }" class="form_field_pair">
                      <div class="form_field_label">
                           <label for="${ coordFieldId }">${ coordFieldName }</label>
                      </div>
                      <div class="form_field_input">
                          <c:if test="${ not crsFieldRequired }">
                              <input type="hidden" name="${ formField.prefix }srid" value="${ formField.crs.srid }" />
                          </c:if>
                          <tiles:insertDefinition name="coordFormField">
                              <tiles:putAttribute name="crs" value="${ formField.crs }"/>
                              <tiles:putAttribute name="isLongitude" value="${ formField.crs.xfirst  }"/>
                              <tiles:putAttribute name="isLatitude" value="${ not formField.crs.xfirst  }" />
                              <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                              <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                              <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
                              <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
                              <tiles:putAttribute name="xCoord" value="${ xCoord }" />
                              <tiles:putAttribute name="yCoord" value="${ yCoord }" />
                              <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                              <tiles:putAttribute name="required" value="${ formField.required }" />
                           </tiles:insertDefinition>
                      </div>
                  </div>

                  <%
                      // declared above
                      //BdrsCoordReferenceSystem crs = ((RecordPropertyFormField)formField).getCrs();
                      if (crs.isXfirst()) {
                          pageContext.setAttribute("coordFieldId", "ff_latitude");
                          pageContext.setAttribute("coordFieldName", crs.getYname());
                      } else {
                          pageContext.setAttribute("coordFieldId", "ff_longitude");
                          pageContext.setAttribute("coordFieldName", crs.getXname());
                      }
                  %>

                  <div id="${ coordFieldId }" class="form_field_pair">
                      <div class="form_field_label">
                          <label for="${ coordFieldId }">${ coordFieldName }</label>
                      </div>
                      <div class="form_field_input">
                          <c:if test="${ not crsFieldRequired }">
                              <input type="hidden" name="${ formField.prefix }srid" value="${ formField.crs.srid }" />
                          </c:if>
                          <tiles:insertDefinition name="coordFormField">
                              <tiles:putAttribute name="crs" value="${ formField.crs }"/>
                              <tiles:putAttribute name="isLongitude" value="${ not formField.crs.xfirst  }"/>
                              <tiles:putAttribute name="isLatitude" value="${ formField.crs.xfirst  }" />
                              <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                              <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                              <tiles:putAttribute name="readOnly" value="${ readOnly }"/>
                              <tiles:putAttribute name="selectedSrid" value="${ selectedSrid }" />
                              <tiles:putAttribute name="xCoord" value="${ xCoord }" />
                              <tiles:putAttribute name="yCoord" value="${ yCoord }" />
                              <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                              <tiles:putAttribute name="required" value="${ formField.required }" />
                          </tiles:insertDefinition>
                      </div>
                  </div>
              </c:if>
          </c:when>
          <c:when test="${ 'Location' == formField.propertyName }">
            <c:if test="${not formField.hidden}">
              <c:if test="${ not empty locations }">
                  <div id="ff_${ form_field_name }" class="form_field_pair">
                      <div class="form_field_label">
                          <label for="location"><cw:validateHtml html="${ formField.description }"/></label>
                      </div>
                      <div class="form_field_input">
                          <tiles:insertDefinition name="propertyRenderer">
                              <tiles:putAttribute name="formField" value="${ formField }"/>
                              <tiles:putAttribute name="locations" value="${ locations }"/>
                              <tiles:putAttribute name="errorMap" value="${ errorMap }"/>
                              <tiles:putAttribute name="valueMap" value="${ valueMap }"/>
                              <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                          </tiles:insertDefinition>
                      </div>
                  </div>
              </c:if>
             </c:if>
          </c:when>
          <c:otherwise>
            <c:if test="${not formField.hidden}">
              <div  id="ff_${ form_field_name }" class="form_field_pair">
                  <div class="form_field_label">
                      <label>
                          <cw:validateHtml html="${ formField.description }"/>
                      </label>
                   </div>
                   <div class="form_field_input">
                       <tiles:insertDefinition name="propertyRenderer">
                           <tiles:putAttribute name="formField" value="${formField}"/>
                           <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                       </tiles:insertDefinition>
                   </div>
               </div>
             </c:if>
           </c:otherwise>
       </c:choose>
    </c:when>
    <c:when test="<%= formField.isAttributeFormField() %>">
        <c:set var="isVisible" value="true"></c:set>
        <div  id="ff_${ form_field_name }" class="form_field_pair">
            <div class="form_field_label">
                <label for="${ formPrefix }attribute_${formField.attribute.id}">
                    <cw:validateHtml html="${formField.attribute.description}"/>
                </label>
            </div>
            <div class="form_field_input">
                <tiles:insertDefinition name="attributeRenderer">
                    <tiles:putAttribute name="formField" value="${formField}"/>
                    <tiles:putAttribute name="editEnabled" value="${ editEnabled }"/>
                </tiles:insertDefinition>
            </div>
        </div>
    </c:when>
</c:choose>
</c:otherwise>
</c:choose>