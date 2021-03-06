package au.com.gaiaresources.bdrs.controller.taxonomy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.ModelAndViewAssert;
import org.springframework.web.servlet.ModelAndView;

import au.com.gaiaresources.bdrs.controller.AbstractControllerTest;
import au.com.gaiaresources.bdrs.controller.record.WebFormAttributeParser;
import au.com.gaiaresources.bdrs.deserialization.record.AttributeParser;
import au.com.gaiaresources.bdrs.json.JSONArray;
import au.com.gaiaresources.bdrs.json.JSONObject;
import au.com.gaiaresources.bdrs.model.preference.Preference;
import au.com.gaiaresources.bdrs.model.preference.PreferenceCategory;
import au.com.gaiaresources.bdrs.model.preference.PreferenceDAO;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeOption;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfile;
import au.com.gaiaresources.bdrs.model.taxa.SpeciesProfileDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxaDAO;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TaxonRank;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.security.Role;
import au.com.gaiaresources.bdrs.service.property.PropertyService;
import au.com.gaiaresources.bdrs.service.web.AtlasService;

public class TaxonomyManagementControllerTest extends AbstractControllerTest {

    private static final List<String> SPECIES_PROFILES = new ArrayList<String>();
    static {
        SPECIES_PROFILES.add("urn:lsid:biodiversity.org.au:afd.taxon:1ed122bb-c4e3-49a6-9281-fcf1c49b1d4b");
        SPECIES_PROFILES.add("urn:lsid:biodiversity.org.au:afd.taxon:a326cf7c-19d1-4714-9bec-61ec3eee0318");
        SPECIES_PROFILES.add("urn:lsid:biodiversity.org.au:afd.taxon:cdddb387-fca5-48d9-85d2-16357c7b986b");
    }
    
    @Autowired
    private TaxaDAO taxaDAO;
    
    @Autowired
    private SpeciesProfileDAO profileDAO;
    
    @Autowired
    private PropertyService propService;

    private TaxonGroup taxonGroupBirds;

    private TaxonGroup taxonGroupButterflies;
    
    private TaxonGroup taxonGroupFrogs;

    private IndicatorSpecies speciesA;

    private IndicatorSpecies speciesB;

    private IndicatorSpecies importSpecies;

    @Autowired
    AtlasService atlasService;

    @Autowired
    PreferenceDAO prefsDAO;
    
    private Map<String, JSONObject> jsonObjs = new HashMap<String, JSONObject>();

    private Map<String, JSONObject> shortJSONObjs = new HashMap<String, JSONObject>();
    
    @Before
    public void setUp() throws Exception {
        taxonGroupBirds = new TaxonGroup();
        taxonGroupBirds.setName("Birds");
        taxonGroupBirds = taxaDAO.save(taxonGroupBirds);

        taxonGroupButterflies = new TaxonGroup();
        taxonGroupButterflies.setName("Butterflies");
        taxonGroupButterflies = taxaDAO.save(taxonGroupButterflies);
        
        taxonGroupFrogs = new TaxonGroup();
        taxonGroupFrogs.setName("Frogs");
        taxonGroupFrogs = taxaDAO.save(taxonGroupFrogs);

        // create census method for census method attributes
        createCensusMethodForAttributes();
        
        List<Attribute> attributeList;
        Attribute attr;
        for (TaxonGroup group : new TaxonGroup[] { taxonGroupBirds,
                taxonGroupButterflies, taxonGroupFrogs }) {
            attributeList = new ArrayList<Attribute>();
            for (AttributeType attrType : AttributeType.values()) {
                attr = new Attribute();
                attr.setRequired(true);
                attr.setName(group.getName() + "_" + attrType.toString());
                attr.setDescription(group.getName() + "_" + attrType.toString());
                attr.setTypeCode(attrType.getCode());
                attr.setScope(null);
                attr.setTag(true);

                if (AttributeType.STRING_WITH_VALID_VALUES.equals(attrType) ||
                        AttributeType.MULTI_CHECKBOX.equals(attrType) ||
                        AttributeType.MULTI_SELECT.equals(attrType)) {
                    List<AttributeOption> optionList = new ArrayList<AttributeOption>();
                    for (int i = 0; i < 4; i++) {
                        AttributeOption opt = new AttributeOption();
                        opt.setValue(String.format("Option %d", i));
                        opt = taxaDAO.save(opt);
                        optionList.add(opt);
                    }  attr = new Attribute();
                    attr.setRequired(true);
                    attr.setName(group.getName() + "_" + attrType.toString());
                    attr.setDescription(group.getName() + "_" + attrType.toString());
                    attr.setTypeCode(attrType.getCode());
                    attr.setScope(null);
                    attr.setTag(true);

                    attr.setOptions(optionList);
                }else if(AttributeType.INTEGER_WITH_RANGE.equals(attrType)){
                    List<AttributeOption> rangeList = new ArrayList<AttributeOption>();
                    AttributeOption upper = new AttributeOption();
                    AttributeOption lower = new AttributeOption();
                    lower.setValue("100");
                    upper.setValue("200");
                    rangeList.add(taxaDAO.save(lower));
                    rangeList.add(taxaDAO.save(upper));
                    attr.setOptions(rangeList);
                } else if (AttributeType.isCensusMethodType(attrType)) {
                    attr.setCensusMethod(attrCm);
                }

                attr = taxaDAO.save(attr);
                attributeList.add(attr);
            }
            group.setAttributes(attributeList);
            taxaDAO.save(group);
        }

        SpeciesProfile profile;
        List<SpeciesProfile> profileList = new ArrayList<SpeciesProfile>();
        for(String profileIndex : new String[]{ "A", "B", "C"}) {
            profile = new SpeciesProfile();
            profile.setType("Profile Type "+profileIndex);
            profile.setContent("Profile Content "+profileIndex);
            profile.setDescription("Profile Description "+profileIndex);
            profile.setHeader("Profile Header "+profileIndex);
            profile = profileDAO.save(profile);
            profileList.add(profile);
        }

        speciesA = new IndicatorSpecies();
        speciesA.setCommonName("Indicator Species A");
        speciesA.setScientificName("Indicator Species A");
        speciesA.setTaxonGroup(taxonGroupBirds);
        speciesA.setInfoItems(profileList);
        speciesA = taxaDAO.save(speciesA);

        profileList = new ArrayList<SpeciesProfile>();
        for(String profileIndex : new String[]{ "X", "Y", "Z"}) {
            profile = new SpeciesProfile();
            profile.setType("Profile Type "+profileIndex);
            profile.setContent("Profile Content "+profileIndex);
            profile.setDescription("Profile Description "+profileIndex);
            profile.setHeader("Profile Header "+profileIndex);
            profile = profileDAO.save(profile);
            profileList.add(profile);
        }
        
        speciesB = new IndicatorSpecies();
        speciesB.setCommonName("Indicator Species B");
        speciesB.setScientificName("Indicator Species B");
        speciesB.setTaxonGroup(taxonGroupButterflies);
        speciesB.setInfoItems(profileList);
        speciesB = taxaDAO.save(speciesB);
        
        // set the preferences for the ala urls to use the file system instead
        Preference alaUrlPref = new Preference();
        alaUrlPref.setKey("ala.species.url");
        alaUrlPref.setValue("file:///"+this.getClass().getResource("profiles").getPath()+"/");
        // Just get the first category.
        PreferenceCategory cat = new PreferenceCategory();
        cat.setDescription("cat desc");
        cat.setDisplayName("catdisplayname");
        cat.setName("catname");
        prefsDAO.save(null, cat);
        
        alaUrlPref.setPreferenceCategory(cat);
        alaUrlPref.setIsRequired(false);
        alaUrlPref.setDescription("Preference for getting the ALA species profiles from the resources.");
        prefsDAO.save(alaUrlPref);
        
        importSpecies = new IndicatorSpecies();
        importSpecies.setCommonName("Indicator Species Import");
        importSpecies.setScientificName("Indicator Species Import");
        importSpecies.setTaxonGroup(taxonGroupButterflies);
        importSpecies.setInfoItems(profileList);
        importSpecies.setSourceId(SPECIES_PROFILES.get(0));
        
        importSpecies = taxaDAO.save(importSpecies);
    }

    @Test
    public void testListing() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/taxonomy/listing.htm");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "taxonomyList");
    }
    
    @Test
    public void testTaxonSearchWithDepth() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/webservice/taxon/searchTaxon.htm");
        request.setParameter("q", speciesA.getCommonName());
        request.setParameter("depth", "2");

        handle(request, response);
        Assert.assertEquals("Content type should be application/json",
                            "application/json", response.getContentType());
    }
    
    @Test
    public void testTaxonByIdWithDepth() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/webservice/taxon/getTaxonById.htm");
        request.setParameter("id", speciesA.getId().toString());
        request.setParameter("depth", "2");

        handle(request, response);
        Assert.assertEquals("Content type should be application/json",
                            "application/json", response.getContentType());
    }
    
    @Test
    public void testAddTaxon() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/taxonomy/edit.htm");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "editTaxon");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxon");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "formFieldList");
    }

    @Test
    public void testAddProfileRow() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/taxonomy/ajaxAddProfile.htm");
        request.setParameter("index", "0");

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "taxonProfileRow");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "index");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "profile");
    }
        
    @Test
    public void testAddTaxonSubmitWithParentLowerLimitOutside() throws Exception{
        testAddTaxon(true, "99");
    }
    
    @Test
    public void testAddTaxonSubmitWithParentLowerLimitEdge() throws Exception {
        testAddTaxon(true, "100");
    }

    @Test
    public void testAddTaxonSubmitWithParent() throws Exception {
        testAddTaxon(true, "101");
    }
    
    @Test
    public void testAddTaxonSubmitWithParentUpperLimitEdge() throws Exception {
        testAddTaxon(true, "200");
    }
    
    @Test
    public void testAddTaxonSubmitWithParentUpperLimitOutside() throws Exception  {
        testAddTaxon(true, "201");
    }
    
    @Test
    public void testAddTaxonSubmitWithoutParentLowerLimitOutside() throws Exception{
        testAddTaxon(false, "99");
    }
    
    @Test
    public void testAddTaxonSubmitWithoutParentLowerLimitEdge() throws Exception {
        testAddTaxon(false, "100");
    }

    @Test
    public void testAddTaxonSubmitWithoutParent() throws Exception {
        testAddTaxon(false, "101");
    }
    
    @Test
    public void testAddTaxonSubmitWithoutParentUpperLimitEdge() throws Exception {
        testAddTaxon(false, "200");
    }
    
    @Test
    public void testAddTaxonSubmitWithoutParentUpperLimitOutside() throws Exception  {
        testAddTaxon(false, "201");
    }

    
    
    
    @Test
    public void testEditTaxon() throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/taxonomy/edit.htm");
        request.setParameter("pk", speciesA.getId().toString());

        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "editTaxon");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "taxon");
        ModelAndViewAssert.assertModelAttributeAvailable(mv, "formFieldList");
    }
    

    @Test
    public void testEditTaxonSubmitWithParentLowerLimitOutside() throws Exception{
        testEditTaxon(true, "99");
    }
    
    @Test
    public void testEditTaxonSubmitWithoutParentLowerLimitOutside() throws Exception{
        testEditTaxon(false, "99");
    }
    
    @Test
    public void testEditTaxonSubmitWithParentLowerLimitEdge() throws Exception {
        testEditTaxon(true, "100");
    }
    
    @Test
    public void testEditTaxonSubmitWithoutParentLowerLimitEdge() throws Exception {
        testEditTaxon(false, "100");
    }

    @Test
    public void testEditTaxonSubmitWithParent() throws Exception {
        testEditTaxon(true, "101");
    }
    
    @Test
    public void testEditTaxonSubmitWithoutParent() throws Exception {
        testEditTaxon(false, "101");
    }
    
    @Test
    public void testEditTaxonSubmitWithParentUpperLimitEdge() throws Exception {
        testEditTaxon(true, "200");
    }
    
    @Test
    public void testEditTaxonSubmitWithoutParentUpperLimitEdge() throws Exception {
        testEditTaxon(false, "200");
    }
    
    @Test
    public void testEditTaxonSubmitWithParentLimitOutside() throws Exception  {
        testEditTaxon(true, "201");
    }
    
    @Test
    public void testEditTaxonSubmitWithoutParentLimitOutside() throws Exception  {
        testEditTaxon(false, "201");
    }
    
    private void testEditTaxon(boolean withParent, String intWithRangeValue) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        Date today = dateFormat.parse(dateFormat.format(new Date(System.currentTimeMillis())));

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/taxonomy/edit.htm");
        
        request.setParameter("taxonPk", speciesA.getId().toString());
        request.setParameter("scientificName", "Test Scientific Name");
        request.setParameter("commonName", "Test Common Name");
        request.setParameter("taxonRank", TaxonRank.SUBSPECIES.toString());
        if(withParent) {
            request.setParameter("parentPk", speciesB.getId().toString());
        } else {
            request.setParameter("parentPk", "");
        }
        request.setParameter("taxonGroupPk", taxonGroupFrogs.getId().toString());
        request.setParameter("author", "Brock Urban");
        request.setParameter("year", "2010");
        
        // Delete a profile, edit a profile and add a profile
        request.addParameter("new_profile", "3");
        request.setParameter("new_profile_type_3", "Test Type 3");
        request.setParameter("new_profile_content_3", "Test Content 3");
        request.setParameter("new_profile_description_3", "Test Description 3");
        request.setParameter("new_profile_header_3", "Test Header 3");
        request.setParameter("new_profile_weight_3", "300");
        
        String profileId = speciesA.getInfoItems().get(0).getId().toString();
        request.addParameter("profile_pk", profileId);
        request.setParameter("profile_type_"+profileId, "Edited Test Type "+profileId);
        request.setParameter("profile_content_"+profileId, "Edited Test Content "+profileId);
        request.setParameter("profile_description_"+profileId, "Edited Test Description "+profileId);
        request.setParameter("profile_header_"+profileId, "Edited Test Header "+profileId);
        request.setParameter("profile_weight_"+profileId, "1400");
        
        int seed = 0;
        Map<String, String> params = new HashMap<String, String>(taxonGroupFrogs.getAttributes().size());
        for (Attribute attr : taxonGroupFrogs.getAttributes()) {
            genRandomAttributeValue(attr, seed++, null, "", params);
        }
        request.addParameters(params);
        
        ModelAndView mv = handle(request, response);

        IndicatorSpecies taxon = taxaDAO.getIndicatorSpecies(speciesA.getId());
        assertRedirect(mv, "/bdrs/admin/taxonomy/listing.htm?taxonPk="+taxon.getId());
        
        Assert.assertEquals(request.getParameter("scientificName"), taxon.getScientificName());
        Assert.assertEquals(request.getParameter("commonName"), taxon.getCommonName());
        Assert.assertEquals(request.getParameter("taxonRank"), taxon.getTaxonRank().toString());
        if(withParent) {
            Assert.assertEquals(request.getParameter("parentPk"), taxon.getParent().getId().toString());
        } else {
            Assert.assertEquals(null, taxon.getParent());
        }
        Assert.assertEquals(request.getParameter("taxonGroupPk"), taxon.getTaxonGroup().getId().toString());
        Assert.assertEquals(request.getParameter("author"), taxon.getAuthor());
        Assert.assertEquals(request.getParameter("year"), taxon.getYear());
        
        Assert.assertEquals(taxon.getInfoItems().size(), 2);
        String index;
        for(SpeciesProfile profile : taxon.getInfoItems()) {
            if(profile.getType().startsWith("Edited")) {
                index = profileId;
                Assert.assertEquals(request.getParameter(String.format("profile_type_%s", index)), profile.getType());
                Assert.assertEquals(request.getParameter(String.format("profile_description_%s", index)), profile.getDescription());
                Assert.assertEquals(request.getParameter(String.format("profile_header_%s", index)), profile.getHeader());
                Assert.assertEquals(request.getParameter(String.format("profile_content_%s", index)), profile.getContent());
                Assert.assertEquals(request.getParameter(String.format("profile_weight_%s", index)), String.valueOf(profile.getWeight()));
            } else {
                String[] split = profile.getType().split(" ");
                index = split[split.length - 1];
                Assert.assertEquals("3", index);
                Assert.assertEquals(request.getParameter(String.format("new_profile_type_%s", index)), profile.getType());
                Assert.assertEquals(request.getParameter(String.format("new_profile_description_%s", index)), profile.getDescription());
                Assert.assertEquals(request.getParameter(String.format("new_profile_header_%s", index)), profile.getHeader());
                Assert.assertEquals(request.getParameter(String.format("new_profile_content_%s", index)), profile.getContent());
                Assert.assertEquals(request.getParameter(String.format("new_profile_weight_%s", index)), String.valueOf(profile.getWeight()));
            }
        }
        
        for(TypedAttributeValue taxonAttr: taxon.getAttributes()) {
            String key = WebFormAttributeParser.getParamKey("", taxonAttr.getAttribute());
            assertAttributes(taxonAttr, params, key);
        }
    }
    
    private void testAddTaxon(boolean withParent, String intWithRangeValue) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });
        
        DateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
        dateFormat.setLenient(false);
        
        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/taxonomy/edit.htm");
        
        request.setParameter("scientificName", "Test Scientific Name");
        request.setParameter("commonName", "Test Common Name");
        request.setParameter("taxonRank", TaxonRank.SUBSPECIES.toString());
        if(withParent) {
            request.setParameter("parentPk", speciesA.getId().toString());
        } else {
            request.setParameter("parentPk", "");
        }
        request.setParameter("taxonGroupPk", taxonGroupButterflies.getId().toString());
        request.setParameter("author", "Anna Abigail");
        request.setParameter("year", "2011");
        
        request.addParameter("new_profile", "2");
        request.setParameter("new_profile_type_2", "Test Type 2");
        request.setParameter("new_profile_content_2", "Test Content 2");
        request.setParameter("new_profile_description_2", "Test Description 2");
        request.setParameter("new_profile_header_2", "Test Header 2");
        request.setParameter("new_profile_weight_2", "200");
        
        request.addParameter("new_profile", "3");
        request.setParameter("new_profile_type_3", "Test Type 3");
        request.setParameter("new_profile_content_3", "Test Content 3");
        request.setParameter("new_profile_description_3", "Test Description 3");
        request.setParameter("new_profile_header_3", "Test Header 3");
        request.setParameter("new_profile_weight_3", "300");
        
        int seed = 0;
        Map<String,String> params = new HashMap<String, String>(taxonGroupButterflies.getAttributes().size());
        for (Attribute attr : taxonGroupButterflies.getAttributes()) {
            genRandomAttributeValue(attr, seed++, null, "", params);
        }
        request.addParameters(params);
        
        ModelAndView mv = handle(request, response);

        IndicatorSpecies taxon = taxaDAO.getIndicatorSpeciesByScientificName(sessionFactory.getCurrentSession(),
                                                                             request.getParameter("scientificName"));

        assertRedirect(mv, "/bdrs/admin/taxonomy/listing.htm?taxonPk="+taxon.getId());
        Assert.assertEquals(request.getParameter("scientificName"), taxon.getScientificName());
        Assert.assertEquals(request.getParameter("commonName"), taxon.getCommonName());
        Assert.assertEquals(request.getParameter("taxonRank"), taxon.getTaxonRank().toString());
        if(withParent) {
            Assert.assertEquals(request.getParameter("parentPk"), taxon.getParent().getId().toString());
        } else {
            Assert.assertEquals(null, taxon.getParent());
        }
        Assert.assertEquals(request.getParameter("taxonGroupPk"), taxon.getTaxonGroup().getId().toString());
        Assert.assertEquals(request.getParameter("author"), taxon.getAuthor());
        Assert.assertEquals(request.getParameter("year"), taxon.getYear());
        
        for(SpeciesProfile profile : taxon.getInfoItems()) {
            String[] split = profile.getType().split(" ");
            String index = split[split.length -1];
            
            Assert.assertEquals(request.getParameter(String.format("new_profile_type_%s", index)), profile.getType());
            Assert.assertEquals(request.getParameter(String.format("new_profile_description_%s", index)), profile.getDescription());
            Assert.assertEquals(request.getParameter(String.format("new_profile_header_%s", index)), profile.getHeader());
            Assert.assertEquals(request.getParameter(String.format("new_profile_content_%s", index)), profile.getContent());
            Assert.assertEquals(request.getParameter(String.format("new_profile_weight_%s", index)), String.valueOf(profile.getWeight()));
        }
        
        for(TypedAttributeValue taxonAttr: taxon.getAttributes()) {
            String key = WebFormAttributeParser.getParamKey("", taxonAttr.getAttribute());
            assertAttributes(taxonAttr, params, key);
        }
    }
    
    @Override
    protected MockHttpServletRequest createMockHttpServletRequest() {
        return super.createUploadRequest();
    }
    
    @Test
    public void testImportOneShortProfile() throws Exception {
        testImportTaxon(SPECIES_PROFILES.get(0), true);
    }
    
    @Test
    public void testImportOneFullProfileOverrideShort() throws Exception {
        testImportTaxon(SPECIES_PROFILES.get(0), false);
    }
    
    @Test
    public void testImportOneFullProfile() throws Exception {
        testImportTaxon(SPECIES_PROFILES.get(1), false);
    }
    
    @Test
    public void testImportMultipleShortProfile() throws Exception {
        testImportTaxon(SPECIES_PROFILES.get(1) + "," +
                        SPECIES_PROFILES.get(2), true);
    }
    
    @Test
    public void testImportMultipleFullProfile() throws Exception {
        testImportTaxon(SPECIES_PROFILES.get(0) + "," +
                        SPECIES_PROFILES.get(2), false);
    }
    
    private void testImportTaxon(String guids, boolean shortProfile) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("POST");
        request.setRequestURI("/bdrs/admin/taxonomy/importNewProfiles.htm");
        
        request.setParameter("guids", guids);
        if (shortProfile) {
            request.setParameter("shortProfile", "on");
        }
        
        handle(request, response);
        
        String[] guidSplit = guids.split(",");
        for (String guid : guidSplit) {
            compareTaxon(guid, shortProfile);
        }
        JSONObject json = JSONObject.fromStringToJSONObject(response.getContentAsString());
        String message = json.getString(TaxonomyManagementController.JSON_KEY_MESSAGE);
        JSONArray errorList = json.getJSONArray(TaxonomyManagementController.JSON_KEY_ERROR_LIST);
        
        Assert.assertNotNull("message cannot be null", message);
        Assert.assertNotNull("error list cannot be null", errorList);
        
        Assert.assertEquals("error count mismatch", 0, errorList.size());
        
        String tmpl = propService.getMessage(TaxonomyManagementController.MSG_KEY_IMPORT_SUCCESS);
        Assert.assertEquals("message mismatch", String.format(tmpl, guidSplit.length), message);
    }

    @Test
    public void testImportFromEdit() throws Exception {
        testImportProfileToTaxon(importSpecies.getId(), null);
    }
    
    @Test
    public void testImportFromEditWithGuid() throws Exception {
        testImportProfileToTaxon(importSpecies.getId(), 
                                 SPECIES_PROFILES.get(2));
    }
    
    private void testImportProfileToTaxon(Integer pk, String guid) throws Exception {
        login("admin", "password", new String[] { Role.ADMIN });

        request.setMethod("GET");
        request.setRequestURI("/bdrs/admin/taxonomy/import.htm");
        
        request.setParameter("pk", String.valueOf(pk));
        if (guid != null) {
            request.setParameter("guid", guid);
        }
        
        ModelAndView mv = handle(request, response);
        ModelAndViewAssert.assertViewName(mv, "editTaxon");
        
        if (guid == null) {
            compareTaxon(importSpecies.getSourceId(), pk);
        } else {
            compareTaxon(guid, pk);
        }
    }
    
    private void compareTaxon(String guid, boolean shortProfile) throws IOException {
        JSONObject ob = getJSONObject(guid, shortProfile);
        IndicatorSpecies expectedTaxon = new IndicatorSpecies();
        if (!shortProfile) {
            expectedTaxon = atlasService.createFullProfile(expectedTaxon, ob, guid, null);
        } else {
            expectedTaxon = atlasService.createShortProfile(expectedTaxon, ob, guid, null);
        }
        IndicatorSpecies actualTaxon = taxaDAO.getIndicatorSpeciesByGuid(guid);
        Assert.assertEquals(guid, actualTaxon.getSourceId());
        compareTaxon(expectedTaxon, actualTaxon);
    }

    private JSONObject getJSONObject(String guid, boolean shortProfile) throws IOException {
        Map<String, JSONObject> jsonObjects = shortProfile ? shortJSONObjs : this.jsonObjs;
        JSONObject ob = jsonObjects.get(guid);
        if (ob == null) {
            ob = atlasService.getJSONObject(guid, shortProfile);
            jsonObjects.put(guid, ob);
        }
        return ob;
    }

    private void compareTaxon(String guid, Integer pk) throws IOException {
        IndicatorSpecies expectedTaxon = taxaDAO.getIndicatorSpeciesByGuid(guid);
        IndicatorSpecies actualTaxon = taxaDAO.getIndicatorSpecies(pk);

        Assert.assertEquals(importSpecies.getId(), pk);
        Assert.assertEquals(guid, actualTaxon.getSourceId());
        compareTaxon(expectedTaxon, actualTaxon);
    }
    
    private void compareTaxon(IndicatorSpecies expectedTaxon, IndicatorSpecies actualTaxon) {
        Assert.assertEquals(expectedTaxon.getScientificName(), actualTaxon.getScientificName());
        Assert.assertEquals(expectedTaxon.getCommonName(), actualTaxon.getCommonName());
        Assert.assertEquals(expectedTaxon.getTaxonRank(), actualTaxon.getTaxonRank());
        
        Assert.assertEquals(expectedTaxon.getTaxonGroup(), actualTaxon.getTaxonGroup());
        Assert.assertEquals(expectedTaxon.getAuthor(), actualTaxon.getAuthor());
        Assert.assertEquals(expectedTaxon.getYear(), actualTaxon.getYear());
        
        Assert.assertTrue(actualTaxon.getInfoItems().containsAll(expectedTaxon.getInfoItems()));
    }
}
