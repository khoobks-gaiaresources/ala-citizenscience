package au.com.gaiaresources.bdrs.model.portal;

import au.com.gaiaresources.bdrs.db.impl.PersistentImpl;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hsqldb.lib.StringUtil;

import javax.persistence.*;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "PORTAL")
@AttributeOverride(name = "id", column = @Column(name = "PORTAL_IDENTIFIER"))
public class Portal extends PersistentImpl {

    @SuppressWarnings("unused")
    private Logger log = Logger.getLogger(getClass());
    
    private String name;
    private boolean isDefault = false;
    private boolean isActive = true;
    private String urlPrefix;

    public Portal() {
        super();
    }

    @Column(name = "NAME", nullable = false, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name = "ISDEFAULT", nullable = false)
    public boolean isDefault() {
        return this.isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Returns true if this portal is currently accessible, false otherwise.
     * @return the isActive true if this portal is currently accessible, false otherwise.
     */
    @Column(name = "ISACTIVE", nullable = false)
    public boolean isActive() {
        return isActive;
    }

    @Column(name = "URL_PREFIX", unique = true)
    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    /**
     * Sets if this portal is accessible.
     * @param isActive true if the portal is accessible, false otherwise.
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Transient
    public String getPortalContextPath() {
        if (StringUtil.isEmpty(getUrlPrefix())) {
            return "/portal/"+getId();
        }
        return "/"+getUrlPrefix();
    }
}
