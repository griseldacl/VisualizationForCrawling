//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml;


/**
 * Java content class for XJBAlternateLocation complex type.
 * 	<p>The following schema fragment specifies the expected 	content contained within this java content object. 	(defined at file:/C:/projects/sourceforge/phex-project/trunk/phex/src/phex/xml/phex.xsd line 541)
 * <p>
 * <pre>
 * &lt;complexType name="XJBAlternateLocation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="host-address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="URN" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface XJBAlternateLocation {


    /**
     * Gets the value of the hostAddress property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getHostAddress();

    /**
     * Sets the value of the hostAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setHostAddress(java.lang.String value);

    /**
     * Gets the value of the urn property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getURN();

    /**
     * Sets the value of the urn property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setURN(java.lang.String value);

}
