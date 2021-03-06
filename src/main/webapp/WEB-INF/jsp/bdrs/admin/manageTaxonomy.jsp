<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<h1>Manage Taxonomy</h1>

<p>
	Taxonomic data underpins your entire Portal, and in this section you can 
	perform a range of different actions to help customise this, either by the 
	individual taxa level, or for the taxonomic group.
</p>
<p>At the level of the individual taxon, you can:</p>
<ul>
	<li><a href="${portalContextPath}/bdrs/admin/taxonomy/edit.htm">Add a new taxon</a></li>
	<li><a href="${portalContextPath}/bdrs/admin/taxonomy/listing.htm">Edit an existing taxon</a></li>
	<li><a href="${portalContextPath}/bdrs/admin/taxonomy/importTaxon.htm">Import taxon information...</a></li>
	<sec:authorize ifAnyGranted="ROLE_ROOT">
	    <li><a href="${portalContextPath}/bdrs/admin/taxonomy/taxonLibImport.htm">Import taxon information from files...</a></li>
    </sec:authorize>
</ul>

<p>At the level of the taxonomic group, you can:</p>
<ul>
	<li><a href="${portalContextPath}/bdrs/admin/taxongroup/edit.htm">Add a new taxonomic group</a></li>
	<li><a href="${portalContextPath}/bdrs/admin/taxongroup/listing.htm">Edit an existing taxonomic group</a></li>
</ul>