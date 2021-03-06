package au.com.gaiaresources.bdrs.controller.record;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.attribute.formfield.FormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordAttributeFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyFormField;
import au.com.gaiaresources.bdrs.controller.attribute.formfield.RecordPropertyType;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.RecordDAO;
import au.com.gaiaresources.bdrs.model.record.RecordVisibility;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.survey.SurveyDAO;
import au.com.gaiaresources.bdrs.model.survey.SurveyFormRendererType;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeScope;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.web.RedirectionService;
import au.com.gaiaresources.bdrs.servlet.BdrsWebConstants;

/**
 * Tests all aspects of the <code>SingleSiteMultiTaxaController</code>.
 */
public class SingleSiteMultiTaxaControllerTest extends RecordFormTest {

    @Autowired
    protected SurveyDAO surveyDAO;
    @Autowired
    protected TaxaDAO taxaDAO;
    @Autowired
    protected MetadataDAO metadataDAO;
    @Autowired
    protected RecordDAO recordDAO;
    @Autowired
    protected RedirectionService redirectionService;

    protected Survey survey;
    protected TaxonGroup taxonGroup;
    protected IndicatorSpecies speciesA;
    protected IndicatorSpecies speciesB;

    @Before
    public void setUp() throws Exception {
        setup(SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA);
    }
    
    protected void setup(SurveyFormRendererType renderType) {
        taxonGroup = new TaxonGroup();
        taxonGroup.setName("Birds");
        taxonGroup = taxaDAO.save(taxonGroup);

        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroup);
        speciesA = taxaDAO.save(speciesA);

        speciesB = new IndicatorSpecies();
        speciesB.setCommonName("Indicator Species B");
        speciesB.setScientificName("Indicator Species B");
        speciesB.setTaxonGroup(taxonGroup);
        speciesB = taxaDAO.save(speciesB);

        // create a census method for census method attribute types
        createCensusMethodForAttributes();
        
        List<Attribute> attributeList = createAttrList("", true, new AttributeScope[] {
                AttributeScope.RECORD, AttributeScope.SURVEY,
                AttributeScope.RECORD_MODERATION, AttributeScope.SURVEY_MODERATION});

        survey = new Survey();
        // make sure that the survey's record visibility is applied...
        survey.setDefaultRecordVisibility(RecordVisibility.CONTROLLED, metadataDAO);
        survey.setName("SingleSiteMultiTaxaSurvey 1234");
        survey.setName(renderType.getName()+" 1234");
        survey.setActive(true);
        survey.setStartDate(new Date());
        survey.setDescription(renderType.getName()+" Survey Description");
        Metadata md = survey.setFormRendererType(renderType);
        metadataDAO.save(md);
        survey.setAttributes(attributeList);
        survey = surveyDAO.save(survey);
    }

    /**
     * Tests that a blank form can be retrieved.
     * 
     * @throws Exception
     */
    @Test
    public void testAddRecord() throws Exception {
        testAddRecord("/bdrs/user/singleSiteMultiTaxa.htm", "singleSiteMultiTaxa");
    }
    
    protected void testAddRecord(String URI, String viewName) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI(URI);
        request.setParameter(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, viewName);
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "survey");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "preview");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        
        RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
        Assert.assertNotNull(formContext.getNamedFormFields().get("formFieldList"));
        Assert.assertNotNull(formContext.getNamedFormFields().get("sightingRowFormFieldList"));

        List<AttributeScope> SURVEY_SCOPES = new ArrayList<AttributeScope>();
        SURVEY_SCOPES.add(AttributeScope.SURVEY);
        SURVEY_SCOPES.add(AttributeScope.SURVEY_MODERATION);
        for (FormField formField : formContext.getNamedFormFields().get("formFieldList")) {
            if (formField.isAttributeFormField()) {
                Assert.assertTrue(SURVEY_SCOPES.contains(((RecordAttributeFormField) formField).getAttribute().getScope()));
            } else if (formField.isPropertyFormField()) {
                String typeName = ((RecordPropertyFormField) formField).getPropertyName();
                // It should not be either the species or the number
                Assert.assertFalse(RecordPropertyType.SPECIES.getName().equals(typeName));
                Assert.assertFalse(RecordPropertyType.NUMBER.getName().equals(typeName));
            } else {
                Assert.assertTrue(false);
            }
        }

        for (FormField formField : formContext.getNamedFormFields().get("sightingRowFormFieldList")) {
            if (formField.isAttributeFormField()) {
                Assert.assertFalse(SURVEY_SCOPES.contains(((RecordAttributeFormField) formField).getAttribute().getScope()));
            } else if (formField.isPropertyFormField()) {
            	String typeName = ((RecordPropertyFormField) formField).getPropertyName();
                // It should not be either the species or the number
                Assert.assertTrue(RecordPropertyType.SPECIES.getName().equals(typeName)
                        || RecordPropertyType.NUMBER.getName().equals(typeName));
            } else {
                Assert.assertTrue(false);
            }
        }
    }

    /**
     * Tests that additional rows can be retrieved. This is normally done via
     * ajax.
     * 
     * @throws Exception
     */
    @Test
    public void testAjaxAddSightingRow() throws Exception {
        testAjaxAddSightingRow("/bdrs/user/singleSiteMultiTaxa/sightingRow.htm", "singleSiteMultiTaxaRow");
    }
    
    public void testAjaxAddSightingRow(String URI, String viewName) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI(URI);

        Map<String, String> param = new HashMap<String, String>();
        param.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        // Try 3 requests
        for (int i = 0; i < 3; i++) {
            param.put("sightingIndex", Integer.valueOf(i).toString());

            request.setParameters(param);

            ModelAndView mv = handle(request, response);
            ModelAndViewAssert.assertViewName(mv, viewName);

            Boolean speciesEditable = (Boolean)mv.getModelMap().get(SingleSiteController.ROW_SPECIES_EDITABLE);
            Assert.assertEquals("wrong species editable flag", this.isSpeciesEditable(), speciesEditable.booleanValue());

            String expectedPrefix = String.format(SingleSiteMultiTaxaController.PREFIX_TEMPLATE, i);
            ModelAndViewAssert.assertModelAttributeAvailable(mv, RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);

            RecordWebFormContext formContext = (RecordWebFormContext)mv.getModel().get(RecordWebFormContext.MODEL_WEB_FORM_CONTEXT);
            Assert.assertNotNull(formContext.getNamedFormFields().get("formFieldList"));
            for (FormField formField : formContext.getNamedFormFields().get("formFieldList")) {
                if (formField.isAttributeFormField()) {

                    RecordAttributeFormField attributeField = (RecordAttributeFormField) formField;
                    Assert.assertEquals(expectedPrefix, attributeField.getPrefix());
                    Assert.assertNull(attributeField.getRecord().getId());
                    Assert.assertEquals(survey, attributeField.getSurvey());

                    Assert.assertFalse(AttributeScope.SURVEY.equals(attributeField.getAttribute().getScope()));

                } else if (formField.isPropertyFormField()) {

                    RecordPropertyFormField propertyField = (RecordPropertyFormField) formField;
                    Assert.assertEquals(expectedPrefix, propertyField.getPrefix());
                    Assert.assertNull(propertyField.getRecord().getId());
                    Assert.assertEquals(survey, propertyField.getSurvey());

                    Assert.assertTrue(RecordPropertyType.SPECIES.getName().equals(propertyField.getPropertyName())
                            || RecordPropertyType.NUMBER.getName().equals(propertyField.getPropertyName()));

                } else {

                    Assert.assertTrue(false);

                }
            }
        }
    }

    /**
     * Tests that multiple records can be saved.
     * 
     * @throws Exception
     */
    @Test 
    public void testSaveRecordLowerLimitOutside() throws Exception{
    	testSaveRecord("99");
    }
    
    @Test 
    public void testSaveRecordLowerLimitEdge() throws Exception{
    	testSaveRecord("100");
    }
    
    @Test 
    public void testSaveRecordInRange() throws Exception{
    	testSaveRecord("101");
    }
    
    @Test 
    public void testSaveRecordUpperLimitEdge() throws Exception{
    	testSaveRecord("200");
    }
    
    @Test 
    public void testSaveRecordUpperLimitOutside() throws Exception{
    	testSaveRecord("201");
    }
    public void testSaveRecord(String intWithRangeValue) throws Exception {
        testSaveRecord(intWithRangeValue, "/bdrs/user/singleSiteMultiTaxa.htm");
    }
    
    protected void testSaveRecord(String intWithRangeValue, String URI) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI(URI);

        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);

        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setLenient(false);
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date sightingDate = cal.getTime();

        Map<String, String> params = new HashMap<String, String>();
        params.put(BdrsWebConstants.PARAM_SURVEY_ID, survey.getId().toString());
        params.put("latitude", "-36.879620605027");
        params.put("longitude", "126.650390625");
        params.put("date", dateFormat.format(sightingDate));
        params.put("time", timeFormat.format(sightingDate));
        params.put("time_hour", new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString());
        params.put("time_minute", new Integer(cal.get(Calendar.MINUTE)).toString());
        params.put("notes", "This is a test record");
        params.put("sightingIndex", "2");

        Map<Attribute, Object> surveyScopeAttributeValueMapping = new HashMap<Attribute, Object>();
        Map<IndicatorSpecies, Map<Attribute, Object>> recordScopeAttributeValueMapping = new HashMap<IndicatorSpecies, Map<Attribute, Object>>(
                2);
        Map<Attribute, Object> attributeValueMapping;

        // We have 2 species set up so lets save them both
        int sightingIndex = 0;
        String surveyPrefix = "";
        for (IndicatorSpecies taxon : new IndicatorSpecies[] { speciesA,
                speciesB }) {
            params.put(String.format("%d_survey_species_search", sightingIndex), taxon.getScientificName());
            params.put(String.format("%d_species", sightingIndex), taxon.getId().toString());
            params.put(String.format("%d_number", sightingIndex), Integer.valueOf(sightingIndex + 21).toString());

            String recordPrefix = String.format("%d_", sightingIndex);
            request.addParameter(SingleSiteController.PARAM_ROW_PREFIX, recordPrefix);
            String prefix;
            attributeValueMapping = new HashMap<Attribute, Object>();
            Map<Attribute, Object> valueMap;
            recordScopeAttributeValueMapping.put(taxon, attributeValueMapping);
            int seed = 0;
            for (Attribute attr : survey.getAttributes()) {
                if(!AttributeScope.LOCATION.equals(attr.getScope())) {
                    if (AttributeScope.isRecordScope(attr.getScope())) {
                        prefix = recordPrefix;
                        valueMap = attributeValueMapping;
                    } else {
                        prefix = surveyPrefix;
                        valueMap = surveyScopeAttributeValueMapping;
                    }
                    AttributeValue av = new AttributeValue();
                    av.setAttribute(attr);
                    genRandomAttributeValue(av, seed++, false, false, valueMap, prefix, params);
                }
            }
            sightingIndex += 1;
        }

        request.setParameters(params);
        ModelAndView mv = handle(request, response);
        Assert.assertEquals(2, recordDAO.countAllRecords().intValue());

        assertRedirect(mv, redirectionService.getMySightingsUrl(survey));

        sightingIndex = 0;
        for (IndicatorSpecies taxon : new IndicatorSpecies[] { speciesA,
                speciesB }) {
            List<Record> records = recordDAO.getRecords(taxon);
            Assert.assertEquals(1, records.size());
            Record record = records.get(0);

            Assert.assertEquals(survey.getId(), record.getSurvey().getId());
            // Coordinates are truncates to 6 decimal points
            Assert.assertEquals(new Double(params.get("latitude")).doubleValue(), record.getPoint().getY(), Math.pow(10, -6));
            Assert.assertEquals(new Double(params.get("longitude")).doubleValue(), record.getPoint().getX(), Math.pow(10, -6));
            Assert.assertEquals(sightingDate, record.getWhen());
            Assert.assertEquals(sightingDate.getTime(), record.getTime().longValue());
            Assert.assertEquals(params.get("notes"), record.getNotes());

            Assert.assertEquals(taxon, record.getSpecies());
            Assert.assertEquals(sightingIndex + 21, record.getNumber().intValue());

            Map<Attribute, Object> attributeValueMap = recordScopeAttributeValueMapping.get(taxon);
            Object expected;
            for (TypedAttributeValue recAttr : record.getAttributes()) {
                if (AttributeScope.SURVEY.equals(recAttr.getAttribute().getScope()) || 
                        AttributeScope.SURVEY_MODERATION.equals(recAttr.getAttribute().getScope())) {
                    expected = surveyScopeAttributeValueMapping.get(recAttr.getAttribute());
                } else {
                    expected = attributeValueMap.get(recAttr.getAttribute());
                }
                assertAttributeValue(recAttr, expected);
            }
            sightingIndex += 1;
        }

        // Test Save and Add Another 
        request.setParameter(RecordWebFormContext.PARAM_SUBMIT_AND_ADD_ANOTHER, "true");
        mv = handle(request, response);
        Assert.assertEquals(4, recordDAO.countAllRecords().intValue());
        
        // get all the records.
        List<Record> recList = recordDAO.search(null, null, null).getList();
        for (Record r : recList) {
            Assert.assertEquals("record should be set to same visibility as the survey default record visibility", survey.getDefaultRecordVisibility(), r.getRecordVisibility());
        }

        assertRedirect(mv, "/bdrs/user/surveyRenderRedirect.htm");
        
        this.assertMessageCode(SingleSiteController.MSG_CODE_SUCCESS_ADD_ANOTHER);
    }
    
    @Test
    public void testRecordFormPredefinedLocationsAsSurveyOwner() throws Exception {
        super.testRecordLocations("/bdrs/user/singleSiteMultiTaxa.htm", true, SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA, true);
    }
    
    @Test
    public void testRecordFormLocationsAsSurveyOwner() throws Exception {
        super.testRecordLocations("/bdrs/user/singleSiteMultiTaxa.htm", false, SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA, true);
    }
    
    @Test
    public void testRecordFormPredefinedLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/singleSiteMultiTaxa.htm", true, SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA, false);
    }
    
    @Test
    public void testRecordFormLocations() throws Exception {
        super.testRecordLocations("/bdrs/user/singleSiteMultiTaxa.htm", false, SurveyFormRendererType.SINGLE_SITE_MULTI_TAXA, false);
    }

    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }

    /**
     * Returns whether the species should be editable or not on an editable form.
     * @return true if species is editable
     */
    protected boolean isSpeciesEditable() {
        return true;
    }
}
