package au.com.gaiaresources.bdrs.model.record.impl;

import java.beans.PropertyDescriptor;
import java.math.BigDecimal;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.persistence.Transient;

import au.com.gaiaresources.bdrs.db.QueryCriteria;
import au.com.gaiaresources.bdrs.model.record.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.type.CustomType;
import org.hibernatespatial.GeometryUserType;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.QueryOperation;
import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.PagedQueryResult;
import au.com.gaiaresources.bdrs.db.impl.PaginationFilter;
import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.db.impl.QueryPaginator;
import au.com.gaiaresources.bdrs.db.impl.SortingCriteria;
import au.com.gaiaresources.bdrs.geometry.GeometryBuilder;
import au.com.gaiaresources.bdrs.model.location.Location;
import au.com.gaiaresources.bdrs.model.metadata.Metadata;
import au.com.gaiaresources.bdrs.model.metadata.MetadataDAO;
import au.com.gaiaresources.bdrs.model.survey.BdrsCoordReferenceSystem;
import au.com.gaiaresources.bdrs.model.survey.Survey;
import au.com.gaiaresources.bdrs.model.taxa.Attribute;
import au.com.gaiaresources.bdrs.model.taxa.AttributeDAO;
import au.com.gaiaresources.bdrs.model.taxa.AttributeType;
import au.com.gaiaresources.bdrs.model.taxa.AttributeValue;
import au.com.gaiaresources.bdrs.model.taxa.IndicatorSpecies;
import au.com.gaiaresources.bdrs.model.taxa.TaxaService;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import au.com.gaiaresources.bdrs.model.taxa.TypedAttributeValue;
import au.com.gaiaresources.bdrs.model.user.User;
import au.com.gaiaresources.bdrs.service.db.DeleteCascadeHandler;
import au.com.gaiaresources.bdrs.service.db.DeletionService;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import au.com.gaiaresources.bdrs.util.Pair;
import au.com.gaiaresources.bdrs.util.StringUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Repository
public class RecordDAOImpl extends AbstractDAOImpl implements RecordDAO {
    private Logger log = Logger.getLogger(getClass());

    @Autowired
    private GeometryBuilder geometryBuilder;
    
    @Autowired
    private DeletionService delService;
    
    @Autowired
    private AttributeDAO attributeDAO;
    
    @Autowired
    private MetadataDAO metaDAO;
    
    @PostConstruct
    public void init() throws Exception {
        delService.registerDeleteCascadeHandler(Record.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                delete((Record)instance);
            }
        });
        delService.registerDeleteCascadeHandler(AttributeValue.class, new DeleteCascadeHandler() {
            @Override
            public void deleteCascade(PersistentImpl instance) {
                attributeDAO.delete((AttributeValue)instance);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Record> getRecords(Survey survey, Set<User> users) {
        if(users.isEmpty()) {
            return Collections.emptyList();
        }

        Query q = getSession().createQuery("select r from Record r where r.user in (:users) and r.survey = :survey");
        q.setParameterList("users", users);
        q.setParameter("survey", survey);
        return q.list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Record> getLatestRecords(User user, String scientificNameSearch, int limit) {
        Map<String,Object> paramMap = new HashMap<String, Object>();
        StringBuilder builder = new StringBuilder();
        builder.append("select r from Record r where r.id > 0");

        if(scientificNameSearch != null && !scientificNameSearch.isEmpty()) {
            // Otherwise you will simply see all records for all species
            builder.append(" and (UPPER(r.species.commonName) like UPPER('%" + StringEscapeUtils.escapeSql(scientificNameSearch) +
                    "%') or UPPER(r.species.scientificName) like UPPER ('%" + StringEscapeUtils.escapeSql(scientificNameSearch) + "%'))");
        }
        if(user != null) {
            // Used if the admin wants to see all records
            builder.append(" and r.user = :user");
            paramMap.put("user", user);
        }

        builder.append(" order by r.when desc");

        Query q = getSession().createQuery(builder.toString());
        for(Map.Entry<String,Object> entry: paramMap.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }
        if(limit > 0) {
            q.setMaxResults(limit);
        }

        return q.list();
    }

    @Override
    public List<Record> getRecords(User user) {
        return find("from Record r where r.user=?", user);
    }

    @Override
    public List<Record> getRecords(User user, Survey survey, Location location, Date startDate, Date endDate) {
        return find("from Record r where r.location = ? and r.survey = ? and r.user = ? and r.when >= ? and r.when <= ?", 
                    new Object[] {location, survey, user, startDate, endDate});
    }
    
    @Override
    public List<Record> getRecordBySurveySpecies(int surveyId, int speciesId) {
        return find("select distinct r from Record r where r.survey.id = ? and r.species.id = ?",
                    new Object[] { surveyId, speciesId });
    }
    
    @Override
    public List<Record> getRecords(Location userLocation) {
        return find("from Record r where r.location = ?", userLocation);
    }
    
    @Override
    public List<Record> getRecordIntersect(Geometry intersectGeom) {
        return this.getRecordIntersect(intersectGeom, 
                                       RecordVisibility.PUBLIC, false);
    }

    @Override
    public List<Record> getRecordIntersect(Geometry intersectGeom,
            RecordVisibility visibility, boolean held) {
        StringBuilder builder = new StringBuilder();
        // use st_ prefix in spatial funcs for macos compatibility
        builder.append("from Record rec where st_intersects(:geom, rec.geometry) = true and rec.recordVisibility = :vis and rec.held = :held");
        Query q = getSession().createQuery(builder.toString());
        q.setParameter("geom", intersectGeom, GeometryUserType.TYPE);
        q.setParameter("vis", visibility);
        q.setParameter("held", held);
        return (List<Record>) q.list();
    }

    @Override
    public List<Record> getRecords(Geometry withinGeom) {
    	if (withinGeom.getSRID() != BdrsCoordReferenceSystem.DEFAULT_SRID) {
    		throw new IllegalArgumentException("geom argument not in default srid, " + withinGeom.getSRID());
    	}

        // use st_ prefix in spatial funcs for macos compatibility
    	Query q = getSession().createQuery("from Record r where st_within(st_transform(r.location.location," + BdrsCoordReferenceSystem.DEFAULT_SRID + "), ?) = true");
    	// the geometry comes first
        CustomType geometryType = new CustomType(GeometryUserType.class, null);
        q.setParameter(0, withinGeom, geometryType);
    	return q.list();
    }

    @Override
    public List<Record> getRecords(IndicatorSpecies species) {
        return newQueryCriteria(Record.class).add("species",
                QueryOperation.EQUAL, species).run();
    }

    @Override
    public Integer countUniqueSpecies() {
        Query q = getSession().createQuery("select count(distinct r.species) from Record r");
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }

    @Override
    public Integer countRecords(User user) {
        RecordFilter filter = new CountRecordFilter();
        filter.setUser(user);
        Query q = filter.getRecordQuery(getSession());
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }

    @Override
    public Integer countAllRecords(User accessor) {
        RecordFilter filter = new CountRecordFilter();
        filter.setAccessor(accessor);
        Query q = filter.getRecordQuery(getSession());
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }
    
    @Override
    public Integer countAllRecords() {
        Query q = getSession().createQuery("select count(*) from Record");
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }
    
    @Override
    public Integer countNullCensusMethodRecords() {
        Query q = getSession().createQuery("select count(*) from Record r where r.censusMethod is null");
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }

    @Override
    public Integer countRecordsForSpecies(IndicatorSpecies species) {
        Query q = getSession().createQuery("select count(*) from Record r where r.species = ?");
        q.setParameter(0, species);
        Integer count = Integer.parseInt(q.list().get(0).toString(),10);
        return count;
    }

    @Override
    public Record getLatestRecord(User user) {

        HqlQuery hqlQuery = new HqlQuery("select r from Record r");
        hqlQuery.and(new Predicate("r.updatedAt != null"));
        if (user != null) {
            hqlQuery.and(Predicate.eq("r.user", user, "myuser"));
        }
        hqlQuery.order("updatedAt", "desc", "r");

        Query q = getSession().createQuery(hqlQuery.getQueryString());
        hqlQuery.applyNamedArgsToQuery(q);

        q.setMaxResults(1);

        List results = q.list();

        if(results.isEmpty()) {
            return null;
        } else {
            return (Record)results.get(0);
        }
    }

    @Override
    public Record getLatestRecord() {
        return getLatestRecord(null);
    }

    @Override
    public Integer countSpecies(User user) {
        return newQueryCriteria(Record.class).add("user",
                QueryOperation.EQUAL, user).countDistinct("species");
    }

    @Override
    public Map<Location, Integer> countLocationRecords(User user) {
        return newQueryCriteria(Record.class).add("user",
                QueryOperation.EQUAL, user).groupByAndCount("location");
    }

    @Override
    public Record getRecord(Integer id) {
        return getByID(Record.class, id);
    }

    @Override
    public Record getRecord(Session sesh, Integer id) {
        return (Record)sesh.get(Record.class, id);
    }
    @Override
    public void deleteById(Integer id) {
        Session sesh = getSession();
        Object ob = sesh.get(Record.class, id);
        if (ob != null)
            // explicitly call the Record delete so we can be sure 
            // related entries are also deleted
            delete((Record)ob);
        else {
            Record record = getRecord(id);
            sesh.delete(record);
        }
    }

    @Override
    public List<Date> getRecordDatesByScientificNameSearch(
            String scientificNameSearch) {
        StringBuilder builder = new StringBuilder();
        builder.append("select r.when from Record r where r.id > 0");

        if(scientificNameSearch != null && !scientificNameSearch.isEmpty()) {
            builder.append(" and (UPPER(r.species.commonName) like UPPER('%" + StringEscapeUtils.escapeSql(scientificNameSearch) +
                    "%') or UPPER(r.species.scientificName) like UPPER ('%" + StringEscapeUtils.escapeSql(scientificNameSearch) + "%'))");
        }
        builder.append(" order by r.when");
        Query q = getSession().createQuery(builder.toString());
        return q.list();
    }
    /**
     * Warning: untested and currently unused method!
     * {@inheritDoc}
     */
    @Override
    public HashSet<Record> getDuplicateRecords(Record record, double bufferMetre, int calendarField, int extendTime, Integer[] excludeRecordIds, Integer[] includeRecordIds){
        Calendar calendar = Calendar.getInstance();
        
        if(Calendar.FIELD_COUNT < calendarField ){
            throw new ArrayIndexOutOfBoundsException(calendarField);
        }
        calendar.setTime(record.getWhen());
        calendar.add(calendarField, -extendTime);
        Date timeFrom = calendar.getTime();
        calendar.setTime(record.getWhen());
        calendar.add(calendarField, extendTime);
        Date timeUntil = calendar.getTime();
        Point point;
        if(record.getPoint()!=null){
            point = record.getPoint();
        } else if(record.getLocation()!= null){
            point = record.getLocation().getLocation().getCentroid();
        } else{
            log.warn("Record Needs to have a point or a location associated with it");
            return new HashSet<Record>();
        }
        Geometry buffer = geometryBuilder.bufferInM(point, bufferMetre);
        
        StringBuilder sb = new StringBuilder("from Record r where");
        // use st_ prefix in spatial funcs for macos compatibility
        sb.append(" st_intersects(st_transform(r.geometry,"+BdrsCoordReferenceSystem.DEFAULT_SRID+"), :buffer) = true");
        sb.append(" and r.when >= :timeFrom");
        sb.append(" and r.when <= :timeUntil");
        sb.append(" and id != :recordId");
        
        if (record.getSpecies() != null) {
        	sb.append(" and r.species = :species");
        } else {
        	sb.append(" and r.species is null");
        }
        if(excludeRecordIds != null && excludeRecordIds.length > 0) {
        	sb.append(" and r.id not in (:excludeRecordIds)");
        }
        if(includeRecordIds!= null && includeRecordIds.length > 0) {
        	sb.append(" and r.id in (:includeRecordIds)");
        }
        
        Query q = getSession().createQuery(sb.toString());
        
        CustomType geometryType = new CustomType(GeometryUserType.class, null);
        q.setParameter("buffer", buffer, geometryType);
        q.setParameter("timeFrom", timeFrom);
        q.setParameter("timeUntil", timeUntil);
        q.setParameter("recordId", record.getId());
        
        if (record.getSpecies() != null) {
        	q.setParameter("species", record.getSpecies());
        }
        if(excludeRecordIds != null && excludeRecordIds.length > 0) {
            q.setParameterList("excludeRecordIds", excludeRecordIds);
        }
        if(includeRecordIds!= null && includeRecordIds.length > 0) {
        	q.setParameterList("includeRecordIds", includeRecordIds);
        }
        return new HashSet(q.list());
    }

    @Override
    public List<Record> getRecord(User user, int groupId, int surveyId,
            int taxonGroupId, Date startDate, Date endDate,
            String speciesScientificNameSearch, int limit) {
        return getRecord(user, groupId, surveyId,
                taxonGroupId, startDate, endDate,
                speciesScientificNameSearch, limit, false);
    }
    
    private Query getRecordQuery(RecordFilter recFilter) {
        List<SortingCriteria> sortCriteria = Collections.emptyList(); 
        return this.getRecordQuery(recFilter, sortCriteria);
    }
    
    @Override
    public int countRecords(RecordFilter recFilter) {
        Query q = recFilter.getRecordQuery(getSession());
        return Integer.parseInt(q.list().get(0).toString(), 10);
    }

    @Override
    public ScrollableRecords getScrollableRecords(User user, int groupPk,
            int surveyPk, int taxonGroupPk, Date startDate, Date endDate,
            String species) {
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setUser(user);
        filter.setGroupPk(groupPk);
        filter.setSurveyPk(surveyPk);
        filter.setTaxonGroupPk(taxonGroupPk);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSpeciesSearch(species);
        
        // There is no limit passed in. If there was however, remember not to 
        // use it in the RecordFilter but on the ScrollableRecords instead.
        
        Query q = getRecordQuery(filter);
        return new ScrollableRecordsImpl(q);
    }
    
    @Override
    public ScrollableRecords getScrollableRecords(User user, int groupPk,
            int surveyPk, int taxonGroupPk, Date startDate, Date endDate,
            String species, int pageNumber, int entriesPerPage) {
        return getScrollableRecords(user, groupPk, surveyPk, taxonGroupPk,
                startDate, endDate, species, pageNumber, entriesPerPage, null, true);
    }

    @Override
    public ScrollableRecords getScrollableRecords(User user, int groupPk, int surveyPk,
                                                  int taxonGroupPk, Date startDate, Date endDate, String species,
                                                  int pageNumber, int entriesPerPage, User accessor,
                                                  boolean roundDateRange) {
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setUser(user);
        filter.setGroupPk(groupPk);
        filter.setSurveyPk(surveyPk);
        filter.setTaxonGroupPk(taxonGroupPk);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSpeciesSearch(species);
        filter.setAccessor(accessor);
        filter.setRoundDateRange(roundDateRange);

        Query q = getRecordQuery(filter);
        return new ScrollableRecordsImpl(q, pageNumber, entriesPerPage);
    }
    
    @Override
    public ScrollableRecords getScrollableRecords(RecordFilter recFilter, List<SortingCriteria> sortCriteria) {
        // Scrollable Records cannot have a max limit set.
        Integer pageNumber = recFilter.getPageNumber();
        Integer entriesPerPage = recFilter.getEntriesPerPage();
        recFilter.setPageNumber(null);
        recFilter.setEntriesPerPage(null);
        
        Query q = getRecordQuery(recFilter, sortCriteria);
        
        recFilter.setPageNumber(pageNumber);
        recFilter.setEntriesPerPage(entriesPerPage);
        
        if (pageNumber != null && entriesPerPage != null) {
            return new ScrollableRecordsImpl(q, pageNumber, entriesPerPage);
        } else {
            return new ScrollableRecordsImpl(q);
        }
    }

    private Query getRecordQuery(RecordFilter recFilter,
            List<SortingCriteria> sortCriteria) {
        Query q = recFilter.getRecordQuery(getSession(), sortCriteria);

        return q;
    }

    @Override
    public ScrollableRecords getScrollableRecords(RecordFilter recFilter) {
        List<SortingCriteria> sortCriteria = Collections.emptyList();
        return getScrollableRecords(recFilter, sortCriteria);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Record> getRecord(User user, int groupId, int surveyId,
            int taxonGroupId, Date startDate, Date endDate,
            String speciesScientificNameSearch, int limit, boolean fetch) {
        
        RecordFilter filter = new AdvancedRecordFilter();
        filter.setUser(user);
        filter.setGroupPk(groupId);
        filter.setSurveyPk(surveyId);
        filter.setTaxonGroupPk(taxonGroupId);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setSpeciesSearch(speciesScientificNameSearch);
        filter.setFetch(fetch);
        filter.setEntriesPerPage(limit);
        
        Query q = getRecordQuery(filter);
        
        return q.list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Record> getRecords(String userRegistrationKey, int surveyPk,
            int locationPk) {
        StringBuilder builder = new StringBuilder();
        builder.append(" select r");
        builder.append(" from Record r");
        builder.append(" where r.survey.id = :surveyPk and");
        builder.append("       r.location.id = :locationPk ");
        if (!StringUtils.nullOrEmpty(userRegistrationKey) && !"null".equals(userRegistrationKey)) {
            builder.append("       and r.user.registrationKey = :regKey");
        }
        builder.append(" order by r.when");

        Query q = getSession().createQuery(builder.toString());
        if (!StringUtils.nullOrEmpty(userRegistrationKey) && !"null".equals(userRegistrationKey)) {
            q.setParameter("regKey", userRegistrationKey);
        }
        q.setParameter("surveyPk", surveyPk);
        q.setParameter("locationPk", locationPk);

        return q.list();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public TypedAttributeValue updateAttribute(Integer id, BigDecimal numeric,
            String value, Date date) {
        AttributeValue att = getByID(AttributeValue.class, id);
        att.setStringValue(value);
        att.setNumericValue(numeric);
        att.setDateValue(date);
        return update(att);
    }

    @Override
    public Record saveRecord(Record r) {
        return save(r);
    }

    @Override
    public AttributeValue saveAttributeValue(AttributeValue recAttr) {
        return save(recAttr);
    }
    
    @Override
    public AttributeValue updateAttributeValue(AttributeValue recAttr) {
        return update(recAttr);
    }
    
    @Override
    public void saveRecordList(List<Record> records) {
        for(Record r : records) {
            save(r);
        }
    }

    @Override
    public Record updateRecord(Record r) {
        return update(r);
    }
    
    @Override
    public  Metadata getRecordMetadataForKey(Record record, String metadataKey){
        for(Metadata md: record.getMetadata()){
            if (md.getKey().equals(metadataKey)){
                return md;
            }
        }
        Metadata md = new Metadata();
        md.setKey(metadataKey);
        return md;
    }
    
    @Override
    public AttributeValue getAttributeValue(int recordAttributePk) {
        return getByID(AttributeValue.class, recordAttributePk);
    }
    
    @Override
    public PersistentImpl getRecordForAttributeValueId(
            Session sesh, Integer id) {
        
        String queryString = "select distinct r from Record r left join r.attributes a where a.id = :id";
        Query q = sesh.createQuery(queryString);
        q.setParameter("id", id);
        
        List<Record> records = q.list();
        
        if(records.isEmpty()) {
            return null;
        } else {
            if(records.size() > 1) {
                log.warn("Multiple records found. Returning the first");

            }
            return records.get(0);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<IndicatorSpecies> getLastSpecies(int userPk, int limit) {
    	List<IndicatorSpecies> species = new ArrayList<IndicatorSpecies>();
    	StringBuilder queryString = new StringBuilder("select distinct i, r.when from Record as r join r.species as i ");
    	if (userPk != 0) {
    		queryString.append("where r.user.id = " + userPk);
    	}
    	queryString.append(" order by r.when desc");
    	Query q = getSession().createQuery(queryString.toString());
    	q.setMaxResults(limit);
    	List<Object[]> resultSet = q.list();
    	for (Object[] result : resultSet) {
    		species.add((IndicatorSpecies)result[0]);
    	}
    	return species;
    }
    
    @Override
    public void delete(Record record) {
        // have to delete child records first
        Set<Record> children = record.getChildRecords();
        for (Record record2 : children) {
            delete(record2);
        }
        // remove the attribute values
        Set<AttributeValue> attributeList = new HashSet<AttributeValue>(record.getAttributes());
        record.getAttributes().clear();
        
        DeleteCascadeHandler cascadeHandler = 
            delService.getDeleteCascadeHandlerFor(AttributeValue.class);
        for(AttributeValue recAttr : attributeList) {
            recAttr = saveAttributeValue(recAttr);
            cascadeHandler.deleteCascade(recAttr);
        }
        
        // remove the metadata
        Set<Metadata> mdSet = new HashSet<Metadata>(record.getMetadata());
        record.getMetadata().clear();
        DeleteCascadeHandler metadataCascadeHandler =
                delService.getDeleteCascadeHandlerFor(Metadata.class);
        for (Metadata md : mdSet) {
            md = metaDAO.save(md);
            metadataCascadeHandler.deleteCascade(md);
        }
        
        // Removing the comments in this way triggers the cascade delete setting on the association to delete
        // the comments associated with the Record.
        record.getComments().clear();
        
        // Force the deletion of the comments before we delete the Record using a query.
        RequestContextHolder.getContext().getHibernate().flush();

        deleteByQuery(record);
    }
    
    @Override
    public void delete(AttributeValue av) {
        attributeDAO.delete(av);
    }
    
    @Override
    public PagedQueryResult<Record> search(PaginationFilter filter, Integer surveyPk, List<Integer> userIdList) {
        HqlQuery q;
        String sortTargetAlias = "r";
        q = new HqlQuery("select r from Record r");
        
        if (surveyPk != null) {
            q.join("r.survey", "survey");
            q.and(Predicate.eq("survey.id", surveyPk));
        }
        if (userIdList != null) {
            if (userIdList.size() > 0) {
                q.join("r.user", "user");
                q.and(Predicate.in("user.id", userIdList.toArray()));
            } else {
                // return empty result
                q.and(Predicate.eq("r.id", 0));
            }
        }
        return new QueryPaginator<Record>().page(this.getSession(), q.getQueryString(), q.getParametersValue(), filter, sortTargetAlias);
    }
    
    @Override
    public List<Pair<TaxonGroup, Long>> getDistinctTaxonGroups(Session sesh) {
    	List<Pair<TaxonGroup, Long>> results = 
                new ArrayList<Pair<TaxonGroup, Long>>();
    	if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
		StringBuilder b = new StringBuilder();
		
		// This query uses a cross join. it may be able to be optimised ?
		b.append(" select g, count(distinct r.id) from TaxonGroup g, ");
		b.append(" Record r left outer join r.species recSpecies left outer join recSpecies.taxonGroup recGroup ");
		b.append(" left outer join r.attributes av left outer join av.species avSpecies ");
		b.append(" left outer join avSpecies.taxonGroup avGroup ");
		b.append(" where ");
		b.append( "(recGroup is not null or avGroup is not null)");
		b.append(" and ( ");
		b.append(" (recGroup = g and avGroup != g) ");
		b.append(" or ");
		b.append(" (recGroup = g and avGroup is null) ");
		b.append(" or " );
		b.append(" (recGroup != g and avGroup = g) ");
		b.append(" or " );
		b.append(" (recGroup is null and avGroup = g) ");
		b.append(" or " );
		b.append(" (recGroup = g and avGroup = g) ");
		b.append(" ) ");
		appendTaxonGroupGroupingClause(b, "g");
		b.append(" order by g.weight asc, g.name asc");
		
		Query q = sesh.createQuery(b.toString());
		
		List<Object[]> queryResult = q.list();
		for (Object[] row : queryResult) {
			TaxonGroup tg = (TaxonGroup)row[0];
			Long count = (Long)row[1];
			results.add(new Pair<TaxonGroup, Long>(tg, count));	
		}
        return results;
    }
    
    private void appendTaxonGroupGroupingClause(StringBuilder b, String taxonGroupAlias) {
    	b.append(" group by " + taxonGroupAlias + ".id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(TaxonGroup.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(Transient.class) == null) &&
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) {
                b.append(", " + taxonGroupAlias + "." + pd.getName());
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocations(org.hibernate.Session, int)
     */
    @Override
    public List<Pair<Location, Long>> getDistinctLocations(Session sesh, int limit) {
        return getDistinctLocations(sesh, limit, null);
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocations(org.hibernate.Session, int, java.lang.Integer[])
     */
    @Override
    public List<Pair<Location, Long>> getDistinctLocations(Session sesh, int limit, Integer[] selectedIds) {
        // first get the matching locations for the selected ids
        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == location, Object[1] == record count
        List<Pair<Location, Long>> results = getSelectedLocations(sesh, selectedIds);
        
        // then get the locations joined to records that are not in the list of selected ids
        // this is done so that locations that are not selected, but also have records
        // will be listed in the count
        StringBuilder b = new StringBuilder();
        b.append(" select l, count(r)");
        b.append(" from Record as r join r.location as l");
        if (selectedIds != null && selectedIds.length > 0) {
            b.append(" where l.id not in (:locids)");
        }
        b.append(" group by l.id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(Location.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(Transient.class) == null) &&
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) {
                b.append(", l."+pd.getName());
            }
        }
        b.append(" order by 2 desc, l.weight asc, l.name asc");

        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());
        if (selectedIds != null && selectedIds.length > 0) {
            q.setParameterList("locids", selectedIds);
        }
        if(limit > 0) {
            q.setMaxResults(limit);
        }
        
        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == location, Object[1] == record count
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<Location, Long>((Location)row[0], (Long)row[1]));
        }
        
        Collections.sort(results, 
                         new Comparator<Pair<Location, Long>>() {
                            @Override
                            public int compare(Pair<Location, Long> o1,
                                    Pair<Location, Long> o2) {
                                // reverse sort the counts
                                return o2.getSecond().compareTo(o1.getSecond());
                            }
                         }
        );
        return results;
    }
    
    /**
     * Returns a list of Locations with the specified ids and their corresponding count of records.
     * @param sesh
     * @param selectedIds
     * @return
     */
    private List<Pair<Location, Long>> getSelectedLocations(Session sesh,
            Integer[] selectedIds) {
        // first get the matching locations for the selected ids
        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == location, Object[1] == record count
        List<Pair<Location, Long>> results = 
            new ArrayList<Pair<Location, Long>>();
        if (selectedIds != null && selectedIds.length > 0) {
            StringBuilder b = new StringBuilder();
            b.append(" select l, count(r)");
            b.append(" from Record as r join r.location as l where l.id in (:locids)");
            b.append(" group by l.id");
            for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(Location.class)) {
                if(!"class".equals(pd.getName()) && 
                    !"id".equals(pd.getName()) && 
                    (pd.getReadMethod().getAnnotation(Transient.class) == null) &&
                    !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) {
                    b.append(", l."+pd.getName());
                }
            }
            b.append(" order by 2 desc, l.weight asc, l.name asc");
            
            if(sesh == null) {
                sesh = super.getSessionFactory().getCurrentSession();
            }
            Query q = sesh.createQuery(b.toString());
            q.setParameterList("locids", selectedIds);
            for(Object rowObj : q.list()) {
                Object[] row = (Object[])rowObj;
                results.add(new Pair<Location, Long>((Location)row[0], (Long)row[1]));
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctUsers(org.hibernate.Session)
     */
    @Override
    public List<Pair<User, Long>> getDistinctUsers(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select u, count(r)");
        b.append(" from Record as r join r.user as u");
        b.append(" group by u.id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(User.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(ForeignKey.class) == null) && // ignore other table joins
                (pd.getReadMethod().getAnnotation(Transient.class) == null || // ignore transients 
                        "active".equals(pd.getName())) &&                     // except active
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) 
            {
                b.append(", u."+pd.getName());
            }
        }
        b.append(" order by 2 desc, u.name asc");

        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == location, Object[1] == record count
        List<Pair<User, Long>> results = 
            new ArrayList<Pair<User, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<User, Long>((User)row[0], (Long)row[1]));
        }
        return results;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctSurveys(org.hibernate.Session)
     */
    @Override
    public List<Pair<Survey, Long>> getDistinctSurveys(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select s, count(distinct r)");
        b.append(" from Record as r join r.survey as s");

        b.append(" group by s.id");
        for(PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(Survey.class)) {
            if(!"class".equals(pd.getName()) && 
                !"id".equals(pd.getName()) && 
                (pd.getReadMethod().getAnnotation(Transient.class) == null) &&
                !(Iterable.class.isAssignableFrom((pd.getReadMethod().getReturnType())))) {
                b.append(", s."+pd.getName());
            }
        }
        b.append(" order by s.weight asc, s.name asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        // Each Object[] has 2 items. Object[0] == taxon group, Object[1] == record count
        List<Pair<Survey, Long>> results = 
            new ArrayList<Pair<Survey, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<Survey, Long>((Survey)row[0], (Long)row[1]));
        }
        return results;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctMonths(org.hibernate.Session)
     */
    @Override
    public List<Pair<Long, Long>> getDistinctMonths(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select distinct month(r.when), count(r)");
        b.append(" from Record as r");

        b.append(" group by month(r.when)");
        b.append(" order by month(r.when) asc");
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());
        
        // Should get back a list of Object[]
        List<Pair<Long, Long>> results = 
            new ArrayList<Pair<Long, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            if (row[0] != null && row[1] != null) {
            	// Month is zero based so we need to subtract by one.
                results.add(new Pair<Long, Long>(Long.parseLong(row[0].toString()), (Long)row[1]));
            }
        }
        return results;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctYears(org.hibernate.Session)
     */
    @Override
    public List<Pair<Long, Long>> getDistinctYears(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select distinct year(r.when), count(r)");
        b.append(" from Record as r");

        b.append(" group by year(r.when)");
        b.append(" order by year(r.when) asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());
        
        // Should get back a list of Object[]
        List<Pair<Long, Long>> results = 
            new ArrayList<Pair<Long, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            if (row[0] != null && row[1] != null) {
	            // Month is zero based so we need to subtract by one.
	            results.add(new Pair<Long, Long>(Long.parseLong(row[0].toString()), (Long)row[1]));
            }
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctCensusMethodTypes(org.hibernate.Session)
     */
    @Override
    public List<Pair<String, Long>> getDistinctCensusMethodTypes(Session sesh) {
        StringBuilder b = new StringBuilder();
        b.append(" select distinct r.censusMethod.type, count(r)");
        b.append(" from Record as r");

        b.append(" group by r.censusMethod.type");
        b.append(" order by r.censusMethod.type asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctAttributeTypes(org.hibernate.Session, au.com.gaiaresources.bdrs.model.taxa.AttributeType[])
     */
    @Override
    public List<Pair<String, Long>> getDistinctAttributeTypes(Session sesh, AttributeType[] attributeTypes) {
        
        StringBuilder b = new StringBuilder();
        b.append(" select distinct a.typeCode, count(distinct r)");
        b.append(" from Record as r join r.attributes as ra join ra.attribute as a");
        b.append(" where length(trim(ra.stringValue)) > 0 and (1 = 2");
        for(AttributeType type : attributeTypes) {
            b.append(String.format(" or a.typeCode = '%s'", type.getCode()));
        }
        b.append(" )");

        b.append(" group by a.typeCode");
        b.append(" order by a.typeCode asc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        
        Query q = sesh.createQuery(b.toString());

        // Should get back a list of Object[]
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctAttributeValues(org.hibernate.Session, java.lang.String, int)
     */
    @Override
    public List<Pair<String, Long>> getDistinctAttributeValues(Session sesh, String attributeName, int limit) {
        
        StringBuilder b = new StringBuilder();
        b.append(" select distinct ra.stringValue, count(distinct r)");
        b.append(" from Record as r join r.attributes as ra join ra.attribute as a");
        b.append(" where ");
        b.append(String.format(" a.description = '%s'", attributeName));
        // ignore empty string values
        b.append(" and ra.stringValue is not null and ra.stringValue != ''");
        b.append(" group by ra.stringValue");
        b.append(" order by 2 desc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        
        Query q = sesh.createQuery(b.toString());

        if (limit > 0) {
            q.setMaxResults(limit);
        }
        
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.facet.FacetDAO#getDistinctLocationAttributeValues(org.hibernate.Session, java.lang.String, int)
     */
    @Override
    public List<Pair<String, Long>> getDistinctLocationAttributeValues(Session sesh, String attributeName, int limit) {
        
        StringBuilder b = new StringBuilder();
        
        b.append(" select distinct locAttrVal.stringValue, count(distinct rec)");
        b.append(" from Record as rec join rec.location as loc join loc.attributes as locAttrVal join locAttrVal.attribute as attr");
        b.append(" where ");
        b.append(String.format(" attr.description = '%s'", attributeName));
        // ignore empty string values
        b.append(" and locAttrVal.stringValue is not null and locAttrVal.stringValue != ''");
        b.append(" group by locAttrVal.stringValue");
        b.append(" order by 2 desc");
        
        if(sesh == null) {
            sesh = super.getSessionFactory().getCurrentSession();
        }
        
        Query q = sesh.createQuery(b.toString());

        if (limit > 0) {
            q.setMaxResults(limit);
        }
        
        List<Pair<String, Long>> results =  new ArrayList<Pair<String, Long>>();
        for(Object rowObj : q.list()) {
            Object[] row = (Object[])rowObj;
            
            results.add(new Pair<String, Long>(row[0].toString(), (Long)row[1]));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pair<RecordVisibility, Long>> getDistinctRecordVisibilities() {

        StringBuilder hql = new StringBuilder();
        hql.append("select distinct recordVisibility, count(r) from Record r group by recordVisibility order by recordVisibility");
        Session session = getSession();
        Query query = session.createQuery(hql.toString());
        
        List<Pair<RecordVisibility, Long>> results = new ArrayList<Pair<RecordVisibility, Long>>();
        for (Object rowObj : query.list()) {
            Object[] row = (Object[])rowObj;
            
            results.add(new Pair<RecordVisibility, Long>((RecordVisibility)row[0], (Long)row[1]));
        }
        return results;
    }
    
    @Override
    public List<Record> find(Integer[] mapLayerId, Geometry intersectGeom, Boolean isPrivate, Integer userId) {
        // To avoid having an empty array which will cause an exception during the query.
        if (mapLayerId.length == 0) {
            mapLayerId = new Integer[] { 0 };
        }
        
        StringBuilder hb = new StringBuilder();
        hb.append("select distinct rec from Record rec inner join rec.survey survey where survey.id in ");
        hb.append(" (select s.id from GeoMapLayer layer inner join layer.survey s where layer.id in (:layerIds)) ");
        if (intersectGeom != null) {
        	if (intersectGeom.getSRID() != BdrsCoordReferenceSystem.DEFAULT_SRID) {
        		throw new IllegalArgumentException("intersect geom must have srid = " + 
        				BdrsCoordReferenceSystem.DEFAULT_SRID +" but was " + intersectGeom.getSRID());
        	}
            // use st_ prefix in spatial funcs for macos compatibility
            hb.append(" and st_intersects(:geom,  st_transform(rec.geometry," + BdrsCoordReferenceSystem.DEFAULT_SRID +")) = true");
        }
        List<String> orSection = new LinkedList<String>();
        if (isPrivate != null) {
            if (isPrivate) {
                orSection.add("rec.recordVisibility = '" + RecordVisibility.OWNER_ONLY + "'");
            } else {
                orSection.add("rec.recordVisibility != '" + RecordVisibility.OWNER_ONLY + "'");
            }
        }
        if (userId != null) {
            orSection.add("rec.user.id = " + userId.toString());
        }
        
        if (orSection.size() > 0) {
            boolean firstItem = true;
            hb.append(" and (");
            for (String clause : orSection) {
                if (firstItem) {
                    firstItem = false;
                } else {
                    hb.append(" or ");
                }
                hb.append(clause);
            }
            hb.append(")");
        }

        Query q = getSession().createQuery(hb.toString());
        q.setParameterList("layerIds", mapLayerId);
        if (intersectGeom != null) {
            q.setParameter("geom", intersectGeom, GeometryUserType.TYPE);
        }
        return (List<Record>)q.list();
    }
    
    public List<Record> getRecords(int count, int offset) {
    	Query q = getSession().createQuery("from Record r order by r.id");
    	q.setMaxResults(count);
    	q.setFirstResult(offset);
    	return q.list();
    }

    @Override
    public Record getRecordByClientID(String clientID) {
        if(clientID == null) {
            throw new NullPointerException();
        }
        
        Session sesh = super.getSessionFactory().getCurrentSession();
        Query q = sesh.createQuery("select distinct r from Record r left join r.metadata md where md.key = :key and md.value = :value");
        q.setParameter("key", Metadata.RECORD_CLIENT_ID_KEY);
        q.setParameter("value", clientID);
        
        q.setMaxResults(1);
        return (Record)q.uniqueResult();
    }
    
    @Override
    public PagedQueryResult<Record> getChildRecords(PaginationFilter filter, Integer parentId, Integer censusMethodId, User accessingUser) {
        
        if (parentId == null) {
            throw new IllegalArgumentException("Integer, parentId, cannot be null");
        }
        
        HqlQuery q;
        String sortTargetAlias = "r";
        
        // need to left join species in the case species is null - we still want the record returned.
        q = new HqlQuery("select r from Record r left join r.species");
        q.and(Predicate.eq("r.parentRecord.id", parentId));

        if (censusMethodId != null) {
            q.and(Predicate.eq("r.censusMethod.id", censusMethodId));
        }
        
        Predicate publicViewPredicate = Predicate.eq("r.recordVisibility", RecordVisibility.PUBLIC).and(Predicate.eq("r.held", false));
        
        // add user visibility parameters here
        if (accessingUser != null) {
            if (!accessingUser.isAdmin()) {
                // logged in user, non admin
                q.and(Predicate.eq("r.user", accessingUser).or(publicViewPredicate));
            }
            // else, admins can see everything and no additional predicate is required
        } else {
            // anonymouse user
            q.and(publicViewPredicate);
        }

        return new QueryPaginator<Record>().page(this.getSession(), q.getQueryString(), q.getParametersValue(), filter, sortTargetAlias);
    }
    
    @Override
    public List<Record> getRecordByAttributeValue(Session sesh, Integer surveyId, String attrName, String attrVal) {
        if (sesh == null) {
            sesh = getSession();
        }
        if (surveyId == null) {
            throw new IllegalArgumentException("Integer cannot be null");
        }
        if (StringUtils.nullOrEmpty(attrName)) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }
        if (StringUtils.nullOrEmpty(attrVal)) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }
        Query q = sesh.createQuery("select distinct r from Record r join r.attributes av where r.survey.id = ? and av.attribute.name = ? and av.stringValue = ?");
        q.setParameter(0, surveyId);
        q.setParameter(1, attrName);
        q.setParameter(2, attrVal);
        return q.list();
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.record.RecordDAO#getRecordByAttribute(org.hibernate.Session, java.lang.Integer, java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<Record> getRecordByAttribute(Session sesh, Integer userId, Integer surveyId, Integer attrId) {
        if (sesh == null) {
            sesh = getSession();
        }
        if (surveyId == null) {
            throw new IllegalArgumentException("Integer cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Integer cannot be null");
        }
        
        Query q = sesh.createQuery("select distinct r, av.stringValue from Record r join r.attributes av join av.attribute attr where r.survey.id = ? and attr.id = ? and r.user.id = ? order by av.stringValue");
        q.setParameter(0, surveyId);
        q.setParameter(1, attrId);
        q.setParameter(2, userId);
        
        List<Object[]> result = q.list();
        List<Record> recList = new ArrayList<Record>(result.size());
        
        for (Object[] objArray : result) {
            recList.add((Record)(objArray[0]));
        }
        
        return recList;
    }
    
    /*
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.record.RecordDAO#getFieldNameRecords(org.hibernate.Session, java.lang.Integer, java.lang.Integer, au.com.gaiaresources.bdrs.model.taxa.TaxaService)
     */
    @Override
    public List<Record> getFieldNameRecords(Session sesh, Integer userId, Integer surveyId, TaxaService taxaService) {
        if (surveyId == null) {
            throw new IllegalArgumentException("Integer cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Integer cannot be null");
        }
        if (taxaService == null) {
            throw new IllegalArgumentException("TaxaService cannot be null");
        }
        IndicatorSpecies fieldNameSpecies = taxaService.getFieldSpecies(sesh);
        Attribute fieldSpeciesAttr = taxaService.getFieldNameAttribute(sesh);
        
        Query q = getSession().createQuery("select distinct r, av.stringValue from Record r join r.attributes av join av.attribute attr where r.survey.id = ? and r.species.id = ? and r.user.id = ? and attr = ? order by av.stringValue");
        q.setParameter(0, surveyId);
        q.setParameter(1, fieldNameSpecies.getId());
        q.setParameter(2, userId);
        q.setParameter(3, fieldSpeciesAttr);
        
        List<Object[]> result = q.list();
        List<Record> recList = new ArrayList<Record>(result.size());
        
        for (Object[] objArray : result) {
            recList.add((Record)(objArray[0]));
        }
        return recList;
    }

    @Override
    public ScrollableRecords getScrollableRecords(User user, List<Survey> surveys,
                                                  List<Integer> species,
                                           Date startDate, Date endDate,
                                           int pageNumber, int entriesPerPage) {

        HqlQuery hqlQuery = new HqlQuery("select r from Record r");

        if (user != null) {
            hqlQuery.and(Predicate.eq("r.user", user, "user"));
        }
        if (surveys != null && !surveys.isEmpty()) {
            hqlQuery.and(Predicate.in("r.survey", surveys, "surveys"));
        }
        if (species != null && !species.isEmpty()) {
            hqlQuery.and(Predicate.in("r.species.id", species, "species"));
        }
        if (startDate != null) {
            hqlQuery.and(Predicate.expr("r.when >= :startDate", startDate, "startDate"));
        }
        if (endDate != null) {
            hqlQuery.and(Predicate.expr("r.when <= :endDate", endDate, "endDate"));
        }

        hqlQuery.order("when", "desc", "r");

        Query query = getSession().createQuery(hqlQuery.getQueryString());

        query.setMaxResults(entriesPerPage);

        hqlQuery.applyNamedArgsToQuery(query);

        return new ScrollableRecordsImpl(query);
    }

    @Override
    public ScrollableRecords getRecordByGroup(RecordGroup group) {

        HqlQuery hqlQuery = new HqlQuery("select r from Record r");
        hqlQuery.and(Predicate.eq("r.recordGroup", group, "group"));
        hqlQuery.order("when", "asc", "r");
        Query query = getSession().createQuery(hqlQuery.getQueryString());
        hqlQuery.applyNamedArgsToQuery(query);
        return new ScrollableRecordsImpl(query);
    }
}




