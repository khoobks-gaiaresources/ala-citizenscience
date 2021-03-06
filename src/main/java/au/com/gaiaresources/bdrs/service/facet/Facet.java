package au.com.gaiaresources.bdrs.service.facet;

import au.com.gaiaresources.bdrs.db.impl.HqlQuery;
import au.com.gaiaresources.bdrs.db.impl.Predicate;
import au.com.gaiaresources.bdrs.service.facet.option.FacetOption;

import java.util.List;

/**
 * The <code>Facet</code> represents a selection criteria to be applied to 
 * the set of records. Facets contain a list of {@link FacetOption}s that
 * represents values to be applied to the selection criteria.  
 */
public interface Facet {

    /**
     * JSON key to retrieve the active state. The active state determines if
     * this facet is applied to search queries.
     */
    public static final String JSON_ACTIVE_KEY = "active";
    /**
     * JSON key to retrieve the weight of the facet. The facet weight determines
     * the sorting order of facets.
     */
    public static final String JSON_WEIGHT_KEY = "weight";
    /**
     * JSON key to retrieve the human readable name of the facet.
     */
    public static final String JSON_NAME_KEY = "name";
    
    /**
     * JSON key to retrieve the prefix for the facet. The prefix is prepended to
     * all inputs for this facet.
     */
    public static final String JSON_PREFIX_KEY = "prefix";

    /**
     * JSON key to retrieve the number of options displayed by default when the facet is rendered.
     */
    public static final String JSON_OPTION_COUNT_KEY = "optionCount";
    /**
     * The default active state. 
     */
    public static final boolean DEFAULT_ACTIVE_CONFIG = true;
    /**
     * The default weight of a facet.
     */
    public static final int DEFAULT_WEIGHT_CONFIG = 0;
    /**
     * The default number of options visible when the facet is rendered
     */
    public static final int DEFAULT_VISIBLE_OPTION_COUNT = 10;
    /**
     * The default prefix that is prepended to all inputs.
     */
    public static final String DEFAULT_PREFIX_CONFIG = "";
    /**
     * A string that describes what the 'name' user configuration parameter will do.
     */
    public static final String NAME_CONFIG_DESCRIPTION = String.format("<dd><code>%s</code> - the displayed name of this facet.", JSON_NAME_KEY);
    /**
     * A string that describes what the 'active' user configuration parameter will do.
     */
    public static final String ACTIVE_CONFIG_DESCRIPTION = String.format("<dd><code>%s</code> - true if this facet should be used, false if this facet should be deactivated. Default = true</dd>", JSON_ACTIVE_KEY);
    /**
     * A string that describes what the 'weight' user configuration parameter will do.
     */
    public static final String WEIGHT_CONFIG_DESCRIPTION = String.format("<dd><code>%s</code> - the sorting index of this facet. Facets are sorted in ascending order. Default = 0</dd>", JSON_WEIGHT_KEY);
    /**
     * A string that describes what the 'optionCount' user configuration parameter will do.
     */
    public static final String OPTION_COUNT_DESCRIPTION = String.format("<dd><code>%s</code> - the number of facet options visible by default. The remaining options can be made visible by the user. Default = 10</dd>", JSON_OPTION_COUNT_KEY);

    /**
     * Returns the name of the base query parameter name.
     * @return the name of the base query parameter name.
     */
    public String getQueryParamName();

    /**
     * Sets the base query parameter name. 
     * @param queryParamName the new query parameter name.
     */
    public void setQueryParamName(String queryParamName);

    /**
     * Returns the human readable name of this facet.
     * @return the human readable name of this facet.
     */
    public String getDisplayName();

    /**
     * Sets the human readable name of this facet.
     * @param displayName the new human readable name of this facet.
     */
    public void setDisplayName(String displayName);

    /**
     * Returns the list of options/predicates associated with this facet.
     * @return the list of options/predicates associated with this facet.
     */
    public List<FacetOption> getFacetOptions();

    /**
     * Sets the list of options/predicates associated with this facet.
     * @param facetOptions the list of options/predicates associated with this facet.
     */
    public void setFacetOptions(List<FacetOption> facetOptions);

    /**
     * Gets the selected predicates from the Facet.
     */
    public Predicate getPredicate();

    /**
     * Sets if at least one of the options in this facet is currently selected.
     * @param containsSelected true if at least one of the options in this facet is currently selected, false otherwise.
     */
    public void setContainsSelected(boolean containsSelected);

    /**
     * Returns true if at least one of the options in this facet is currently selected, false otherwise.
     * @return true if at least one of the options in this facet is currently selected, false otherwise.
     */
    public boolean isContainsSelected();
    
    /**
     * Returns true if there are more than zero {@link FacetOption}s and 
     * all options in this facet are currently selected, false otherwise.
     * @return true if there are more than zero {@link FacetOption}s and 
     * all options in this facet are currently selected, false otherwise.
     */
    public boolean isAllSelected();
    
    /**
     * Returns true if the Facet is active and should be shown in the UI.
     * @return true if the Facet is active and should be shown in the UI
     */
    public boolean isActive();
    
    /**
     * Returns the sorting weight of this facet.
     * @return the sorting weight of this facet.
     */
    public int getWeight();
    
    /**
     * Returns the name of the input by prepending to the prefix to the query
     * parametrs name.
     * @return the name of the inputs for this facet.
     */
    public String getInputName();
    
    /**
     * Returns the prefix to be appended before all inputs.
     * @return the prefix to be appended before all inputs.
     */
    public String getPrefix();

    /**
     * To be overridden by implementors if any custom table joins are required
     * before the predicate is applied. 
     */
    public void applyCustomJoins(HqlQuery query);

    /**
     * Returns a query string for performing a lucene-based indexed query.
     * @return a query string for performing a lucene-based indexed query.
     */
    public String getIndexedQueryString();

    /**
     * The number of options displayed by default when the facet is rendered.  If there are more options available
     * they will be made visible when the "show more options" link is clicked.
     * @return the number of options to display by default for this Facet.
     */
    public int getDefaultVisibleOptionCount();

    /**
     * The expanded property tracks whether all FacetOptions for this Facet should be visible or not.
     * @return true if this Facet has been expanded, that is, all FacetOptions should be displayed.  Otherwise only
     * defaultVisibleOptionCount FacetOptions should be displayed.
     */
    public boolean isExpanded();

    /**
     * The expanded property tracks whether all FacetOptions for this Facet should be visible or not.
     * Sets whether this Facet has been expanded or not.
     * @param expanded true if this Facet as been expanded.
     */
    public void setExpanded(boolean expanded);

    /**
     * @return the name of the parameter that contains the values of selected options relevant to this Facet.
     */
    public String getOptionsParameterName();

    /**
     * @return the name of the parameter that contains the value of the expanded property for this Facet.
     */
    public String getExpandedParameterName();
}
