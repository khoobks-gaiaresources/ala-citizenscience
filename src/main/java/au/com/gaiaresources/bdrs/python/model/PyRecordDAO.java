package au.com.gaiaresources.bdrs.python.model;

import java.util.*;

import au.com.gaiaresources.bdrs.json.JSONArray;

import au.com.gaiaresources.bdrs.model.taxa.*;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.ScrollableRecords;
import au.com.gaiaresources.bdrs.model.record.impl.AdvancedRecordFilter;
import au.com.gaiaresources.bdrs.model.record.impl.RecordFilter;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

/**
 * Represents a facade over the {@link RecordDAO} ensuring that any data
 * retrieved using this facade is readonly.
 */
public class PyRecordDAO extends AbstractPyDAO {
    private Logger log = Logger.getLogger(getClass());

    private User accessor;
    private RecordDAO recordDAO;

    /**
     * Creates a new instance.
     * 
     * @param user
     *            the user accessing data.
     * @param recordDAO
     *            retrieves record related data.
     */
    public PyRecordDAO(User user, RecordDAO recordDAO) {
        this.accessor = user;
        this.recordDAO = recordDAO;
    }

    /**
     * Returns a JSON serialized record with the specified primary key.
     *
     * @param recordId the primary key of the record to be returned.
     * @return a JSON serialized record with the specified primary key.
     */
    public String getRecordById(int recordId) {
        return PyDAOUtil.toJSON(recordDAO.getRecord(recordId)).toString();
    }

    @Override
    public String getById(int pk) {
        return super.getById(recordDAO, Record.class, pk);
    }

    /**
     * Returns all records for the specified survey.
     * 
     * @param surveyId
     *            the primary key of the survey containing the desired records.
     * @param includeTaxon
     *            true if the serialized record should contain a serialized
     *            taxon, or false if only a primary key is required.
     * @param includeLocation
     *            true if the serialized record should contain a serialized
     *            location, or false if only a primary key is required.
     * @return a JSON serialized representation of the records contained by the
     *         specified survey.
     * 
     * @deprecated If possible, use
     *             {@link #getScrollableRecordsForSurvey(int, boolean, boolean)}
     *             instead.
     */
    public String getRecordsForSurvey(int surveyId, boolean includeTaxon,
            boolean includeLocation) {
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setSurveyPk(surveyId);
        filter.setAccessor(accessor);

        ScrollableRecords sr = recordDAO.getScrollableRecords(filter);

        int count = 0;
        Session sesh = RequestContextHolder.getContext().getHibernate();
        JSONArray array = new JSONArray();
        while (sr.hasMoreElements()) {
            Record rec = sr.nextElement();
            Map<String, Object> recFlatten = rec.flatten();

            if (includeTaxon) {
                IndicatorSpecies species = rec.getSpecies();
                if (species != null) {
                    recFlatten.put("species", species.flatten());
                }
            }

            if (includeLocation) {
                Location loc = rec.getLocation();
                if (loc != null) {
                    recFlatten.put("location", loc.flatten());
                }
            }

            array.add(recFlatten);

            // evict to ensure garbage collection
            if (++count % ScrollableRecords.RESULTS_BATCH_SIZE == 0) {
                sesh.clear();
            }
        }
        return array.toString();
    }

    /**
     * Returns all records for the specified survey.
     * 
     * @param surveyId
     *            the primary key of the survey containing the desired records.
     * @param includeTaxon
     *            true if the serialized record should contain a serialized
     *            taxon, or false if only a primary key is required.
     * @param includeLocation
     *            true if the serialized record should contain a serialized
     *            location, or false if only a primary key is required.
     * @return a JSON serialized representation of the records contained by the
     *         specified survey.
     */
    public PyScrollableRecords getScrollableRecordsForSurvey(int surveyId,
            boolean includeTaxon, boolean includeLocation) {
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setSurveyPk(surveyId);
        filter.setAccessor(accessor);

        return new PyScrollableRecords(recordDAO.getScrollableRecords(filter),
                includeTaxon, includeLocation);
    }

    /**
     * Returns all records for the specified survey.
     * 
     * @param surveyId
     *            the primary key of the survey containing the desired records.
     * @param includeTaxon
     *            true if the serialized record should contain a serialized
     *            taxon, or false if only a primary key is required.
     * @param includeLocation
     *            true if the serialized record should contain a serialized
     *            location, or false if only a primary key is required.
     * @param includeAttributeValues
     *            true if the serialized record should contain serialized
     *            attribute values, or false if only a primary key is required.
     * @return a JSON serialized representation of the records contained by the
     *         specified survey.
     */
    public PyScrollableRecords getScrollableRecordsForSurvey(int surveyId,
            boolean includeTaxon, boolean includeLocation,
            boolean includeAttributeValues) {
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setSurveyPk(surveyId);
        filter.setAccessor(accessor);

        return new PyScrollableRecords(recordDAO.getScrollableRecords(filter),
                includeTaxon, includeLocation, includeAttributeValues);
    }

    /**
     * Retrieves all records within the geometry specified.
     *
     * @param srid
     *            the projection of the wkt string
     * @param wktFilter
     *            the wkt string that defines the geometry
     * @return a json serialized representation of the records contained by the
     *         geometry.
     */
    public String getRecordsWithinGeometry(int srid, String wktFilter) {
        WKTReader fromText = new WKTReader();
        Geometry filter = null;
        try {
            filter = fromText.read(wktFilter);
            filter.setSRID(srid);
            if(filter.isValid()) {
                return PyDAOUtil.toJSON(recordDAO.getRecordIntersect(filter)).toString();
            } else {
                log.error(String.format("WKT String does not produce a valid geometry: %s", wktFilter));
                return null;
            }

        } catch (ParseException e) {
            log.error(String.format("Failed to parse WKT String: %s", wktFilter), e);
            return null;
        }
    }

    /**
     * Returns a JSON encoded list of Records with the same values for Survey scoped Attributes. Note that this
     * list will include the candidate record.
     *
     * @param recordId the primary key of the candidate record.
     * @return a JSON encoded list of Records with the same values for Survey scoped Attributes.
     */
    public String getSurveyScopeCompanionRecords(int recordId) {
        List<Record> result = new ArrayList<Record>();
        Record rec = recordDAO.getRecord(recordId);

        if (rec != null) {
            // Build a attribute to attribute value mapping of survey scoped attributes for the source record.
            // Making it unmodifiable so you cannot accidentally mess with it.
            //
            Map<Attribute, AttributeValue> attrMap = Collections.unmodifiableMap(buildSurveyScopeAttrMap(rec));

            AdvancedRecordFilter recFilter = new AdvancedRecordFilter();
            recFilter.setStartDate(rec.getWhen());
            recFilter.setEndDate(rec.getWhen());
            recFilter.setSurveyPk(rec.getSurvey().getId());
            recFilter.setUser(rec.getUser());
            recFilter.setAccessor(accessor);

            ScrollableRecords scrollableRecords = recordDAO.getScrollableRecords(recFilter);

            while (scrollableRecords.hasMoreElements()) {
                boolean isSimilar = true;
                Record testRecord = scrollableRecords.nextElement();
                Map<Attribute, AttributeValue> testAttrMap = buildSurveyScopeAttrMap(testRecord);

                // Make sure the geometry is the same.
                isSimilar = isSimilar && isEqualGeometry(rec.getGeometry(), testRecord.getGeometry());

                for (Map.Entry<Attribute, AttributeValue> entry : attrMap.entrySet()) {
                    AttributeValue testAttrVal = testAttrMap.remove(entry.getKey());
                    isSimilar = isSimilar && testAttrVal != null &&
                            AttributeValueUtil.isAttributeValuesEqual(entry.getValue(), testAttrVal);
                }

                // Assert that after removing all survey scoped attributes, the test record does not have any
                // extra attributes that are not present in the source record.
                isSimilar = isSimilar && testAttrMap.isEmpty();

                if (isSimilar) {
                    result.add(testRecord);
                }
            }
        }

        return PyDAOUtil.toJSON(result).toString();
    }
    
    /**
     * Get records for a survey with the matching species id.
     * 
     * @param surveyId Survey ID to query for.
     * @param speciesId IndicatorSpecies ID to query for.
     * @return List of records
     */
    public String getRecordsBySurveyAndSpecies(int surveyId, int speciesId) {
        List<Record> result = recordDAO.getRecordBySurveySpecies(surveyId, speciesId);
        return PyDAOUtil.toJSON(result).toString();
    }
    
    /**
     * Get records for a survey where the attribute for a given name matches a given value.
     * Nulls not handled.
     * 
     * @param surveyId Survey ID to search for.
     * @param attrName Attribute name to search for.
     * @param attrVal AttributeValue.stringValue to search for.
     * @return
     */
    public String getRecordsByAttributeValue(int surveyId, String attrName, String attrVal) {
        List<Record> result = recordDAO.getRecordByAttributeValue(null, surveyId, attrName, attrVal);
        return PyDAOUtil.toJSON(result).toString();
    }

    /**
     * Builds a map of Attribute to AttributeValues where each attribute is either
     * {@link AttributeScope#SURVEY Survey} scoped or {@link AttributeScope#SURVEY_MODERATION Survey Moderation}
     * scoped.
     *
     * @param rec the record containing the attribute values to be mapped.
     * @return a mapping of Attribute to AttributeValues.
     */
    private Map<Attribute, AttributeValue> buildSurveyScopeAttrMap(Record rec) {
        Map<Attribute, AttributeValue> surveyScopeAttrMap = new HashMap<Attribute, AttributeValue>(rec.getAttributes().size());
        for (AttributeValue attrVal : rec.getAttributes()) {
            Attribute attr = attrVal.getAttribute();
            AttributeScope scope = attr.getScope();
            if (AttributeScope.SURVEY.equals(scope) || AttributeScope.SURVEY_MODERATION.equals(scope)) {
                surveyScopeAttrMap.put(attrVal.getAttribute(), attrVal);
            }
        }
        return surveyScopeAttrMap;
    }

    /**
     * Tests the equality of two geometries.
     *
     * @param geom  the first geometry
     * @param other the other geometry
     * @return true if both geometries are null or exactly equal, false otherwise.
     */
    private boolean isEqualGeometry(Geometry geom, Geometry other) {
        return (geom == null && other == null) || (geom != null && other != null && geom.equalsExact(other));
    }
}
