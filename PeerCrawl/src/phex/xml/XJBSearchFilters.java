//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml;


/**
 * Java content class for XJBSearchFilters complex type.
 * 	<p>The following schema fragment specifies the expected 	content contained within this java content object. 	(defined at file:/C:/projects/sourceforge/phex-project/trunk/phex/src/phex/xml/phex.xsd line 104)
 * <p>
 * <pre>
 * &lt;complexType name="XJBSearchFilters">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="search-filter" type="{}XJBSearchFilter" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface XJBSearchFilters {


    /**
     * Gets the value of the SearchFilterList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the SearchFilterList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSearchFilterList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link phex.xml.XJBSearchFilter}
     * 
     */
    java.util.List getSearchFilterList();

}