package au.com.gaiaresources.bdrs.db.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.beans.factory.annotation.Autowired;

import au.com.gaiaresources.bdrs.db.FilterManager;
import au.com.gaiaresources.bdrs.db.Persistent;
import au.com.gaiaresources.bdrs.db.QueryCriteria;
import au.com.gaiaresources.bdrs.db.TransactionDAO;
import au.com.gaiaresources.bdrs.model.portal.Portal;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;

public abstract class AbstractDAOImpl implements TransactionDAO {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(AbstractDAOImpl.class);

    @Autowired
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public <T extends Persistent> T save(T instance) {
        updateTimestamp(instance);
        return save(sessionFactory.getCurrentSession(), instance);
    }

    public <T extends Persistent> T save(Session sesh, T instance) {
        if (sesh == null) {
            sesh = sessionFactory.getCurrentSession();
        }
        updateTimestamp(instance);
        sesh.save(instance);
        return instance;
    }
    
    public <T extends Persistent> T saveOrUpdate(Session sesh, T instance) {
        updateTimestamp(instance);
        sesh.saveOrUpdate(instance);
        return instance;
    }
    
    @Override
    public <T extends Persistent> T saveOrUpdate(T instance) {
        return saveOrUpdate(sessionFactory.getCurrentSession(), instance);
    }
    
    protected int deleteByQuery(Persistent instance) {
        if(instance.getId() == null) {
            return 0;
        }
        
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("id", instance.getId());
        String clazzName = HibernateProxyHelper.getClassWithoutInitializingProxy(instance).getSimpleName();
        String queryString = String.format("delete %s where id = :id", clazzName);
        log.debug(queryString.replaceAll(":id", instance.getId().toString()));
        return execute(queryString, params);
    }
    
    public int execute(String query, Map<String, Object> params) {
        return execute(getSessionFactory().getCurrentSession(), query, params);
    }
    
    public int execute(Session sesh, String query, Map<String, Object> params) {
        Query q = sesh.createQuery(query);
        for(Map.Entry<String, Object> entry : params.entrySet()) {
            q.setParameter(entry.getKey(), entry.getValue());
        }
        return q.executeUpdate();
    }
    
    public <T extends Persistent> void delete(Session sesh, T instance) {
        sesh.delete(instance);
    }
    

    public <T extends Persistent> void delete(T instance) {
        this.delete(sessionFactory.getCurrentSession(),instance);
    }

    public <T extends Persistent> T update(T instance) {
        updateTimestamp(instance);
        return update(sessionFactory.getCurrentSession(), instance);
    }

    public <T extends Persistent> T update(Session sesh, T instance) {
        updateTimestamp(instance);
        sesh.update(instance);
        return instance;
    }
    
    public <T extends Persistent> Long count(Class<T> clazz) { 
        String queryString = String.format("select count(*) from %s", clazz.getSimpleName());
        return (Long)sessionFactory.getCurrentSession().createQuery(queryString).iterate().next();
    }
    
    public <T extends Persistent> void refresh(T instance) {
    	refresh(this.getSession(), instance);
    }
    
    public <T extends Persistent> void refresh(Session sesh, T instance) {
    	sesh.refresh(instance);
    }
    
    private void updateTimestamp(Persistent persistent) {
        persistent.setUpdatedAt(new Date());
    }

    @SuppressWarnings("unchecked")
    protected <T extends PersistentImpl> T merge(T instance) {
        T ob = (T)sessionFactory.getCurrentSession().merge(instance);
        return ob;
    }
    
    @SuppressWarnings("unchecked")
    protected <T extends PersistentImpl> T getByID(Session sesh, Class<T> clazz, Integer id) {
        // The following subterfuge is required because hibernate does not apply
        // filters to 'get' requests.
        // https://forum.hibernate.org/viewtopic.php?f=1&t=966610
        if(PortalPersistentImpl.class.isAssignableFrom(clazz)) {
            List<PersistentImpl> list = this.find(sesh,
                                                  String.format("from %s where id = ?", clazz.getSimpleName()), new Object[]{id}, 1);
            return list.isEmpty() ? null : (T)list.get(0);
        } else {
            return (T)sesh.get(clazz, id);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends PersistentImpl> T getByID(Class<T> clazz, Integer id) {
        return getByID(sessionFactory.getCurrentSession(), clazz, id);
    }

    protected <T extends PersistentImpl> QueryCriteria<T> newQueryCriteria(Class<T> persistentClass) {
        return new QueryCriteriaImpl<T>(sessionFactory.getCurrentSession().createCriteria(persistentClass));
    }

    protected <T extends Persistent> List<T> find(Session sesh, String hql, Object[] args, int limit)
    {
        Query query = sesh.createQuery(hql);
        for (int i = 0; i < args.length; i++) {
            query.setParameter(i, args[i]);
        }
        query.setMaxResults(limit);
        return query.list();
    }

    protected <T extends Persistent> List<T> find(String hql, Object[] args, int limit)
    {
        return find(sessionFactory.getCurrentSession(), hql, args, limit);
    }

    protected <T extends Persistent> List<T> find(Session sesh, String hql, Object[] args)
    {
        if (sesh == null) {
            sesh = sessionFactory.getCurrentSession();
        }
        Query query = sesh.createQuery(hql);
        for (int i = 0; i < args.length; i++) {
            query.setParameter(i, args[i]);
        }
        return query.list();
    }

    protected <T extends Persistent> List<T> find(String hql, Object[] args)
    {
        return find(sessionFactory.getCurrentSession(), hql, args);
    }
        
    /**
     * Returns a unique result if one item is found, null if 0 items are found. Throws an exception
     * if more than 1 item is found.
     * 
     * @param <T>
     * @param hql for the query
     * @param args - positional args for the hql query
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T extends Persistent> T findUnique(String hql, Object[] args) {
        return (T)findUnique(sessionFactory.getCurrentSession(), hql, args);
    }
    
    /**
     * Returns a unique result if one item is found, null if 0 items are found. Throws an exception
     * if more than 1 item is found.
     * 
     * @param <T>
     * @param sesh - session to execute the query
     * @param hql for the query
     * @param args - positional args for the hql query
     * @return
     */
    protected <T extends Persistent> T findUnique(Session sesh, String hql, Object[] args) {
        
        if (sesh == null) {
            throw new IllegalArgumentException("Session, sesh, cannot be null");
        }
        if (hql == null) {
            throw new IllegalArgumentException("String, hql, cannot be null");
        }
        
        List<T> list = find(sesh, hql, args);
        if (list.size() > 1) {
            throw new IllegalStateException("Unique result should be returned. Instead we have : " + list.size());
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        return null;
    }

    protected <T extends Persistent> List<T> find(Session sesh, String hql, Object args)
    {
        return find(sesh, hql, new Object[]{args});
    }

    protected <T extends PersistentImpl> List<T> find(String hql, Object args)
    {
        return find(sessionFactory.getCurrentSession(), hql, new Object[]{args});
    }

    protected <T extends PersistentImpl> List<T> find(Session sesh, String hql)
    {
        return find(sesh, hql, new Object[0]);
    }

    protected <T extends PersistentImpl> List<T> find(String hql)
    {
        return find(sessionFactory.getCurrentSession(), hql, new Object[]{});
    }

    protected Session getSession()
    {
        return sessionFactory.getCurrentSession();
    }
    
    protected void enablePortalFilter() {
        Portal portal = RequestContextHolder.getContext().getPortal();
        // portal can be null on root portal initialization
        if (portal != null) {
            FilterManager.setPortalFilter(getSession(), portal);
        }
    }
    
    protected void disablePortalFilter() {
        getSession().disableFilter(PortalPersistentImpl.PORTAL_FILTER_NAME);
    }
}
