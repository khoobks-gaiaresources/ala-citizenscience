package au.com.gaiaresources.bdrs.service.facet.option;

import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.model.taxa.TaxonGroup;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Represents a single {@link TaxonGroup}
 */
public class TaxonGroupFacetOption extends FacetOption {
    
    private TaxonGroup taxonGroup;
    private String speciesAttributeGroupAlias;

    /**
     * Creates a new instance of this class.
     * 
     * @param taxonGroup the human readable name of this option.
     * @param count the number of records that match this option.
     * @param selectedOpts true if this option is applied, false otherwise.
     */
    public TaxonGroupFacetOption(TaxonGroup taxonGroup, Long count, String[] selectedOpts, String speciesAttributeGroupAlias) {
        super(taxonGroup.getName(), String.valueOf(taxonGroup.getId()), count, 
              Arrays.binarySearch(selectedOpts, String.valueOf(taxonGroup.getId())) > -1);
        
        this.speciesAttributeGroupAlias = speciesAttributeGroupAlias;
        this.taxonGroup = taxonGroup;
    }

    /**
     * Returns the predicate represented by this option that may be applied to a
     * query.
     */
    public Predicate getPredicate() {
        return Predicate.enclose(
        		Predicate.eq("species.taxonGroup.id", taxonGroup.getId()).or(
        				Predicate.eq(speciesAttributeGroupAlias+".id", taxonGroup.getId())));
    }
}
