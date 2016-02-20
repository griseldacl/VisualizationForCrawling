//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml;


/**
 * Java content class for XJBSearchFilter complex type.
 * 	<p>The following schema fragment specifies the expected 	content contained within this java content object. 	(defined at file:/C:/projects/sourceforge/phex-project/trunk/phex/src/phex/xml/phex.xsd line 115)
 * <p>
 * <pre>
 * &lt;complexType name="XJBSearchFilter">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="refine-text" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="min-file-size" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="min-host-rating" type="{http://www.w3.org/2001/XMLSchema}short" minOccurs="0"/>
 *         &lt;element name="min-host-speed" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="last-time-used" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="media-type" type="{}XJBMediaType" default="any" />
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface XJBSearchFilter {


    /**
     * Gets the value of the minHostRating property.
     * 
     */
    short getMinHostRating();

    /**
     * Sets the value of the minHostRating property.
     * 
     */
    void setMinHostRating(short value);

    /**
     * Gets the value of the minHostSpeed property.
     * 
     */
    int getMinHostSpeed();

    /**
     * Sets the value of the minHostSpeed property.
     * 
     */
    void setMinHostSpeed(int value);

    /**
     * Gets the value of the lastTimeUsed property.
     * 
     */
    long getLastTimeUsed();

    /**
     * Sets the value of the lastTimeUsed property.
     * 
     */
    void setLastTimeUsed(long value);

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getName();

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setName(java.lang.String value);

    /**
     * Gets the value of the minFileSize property.
     * 
     */
    long getMinFileSize();

    /**
     * Sets the value of the minFileSize property.
     * 
     */
    void setMinFileSize(long value);

    /**
     * Gets the value of the mediaType property.
     * 
     * @return
     *     possible object is
     *     {@link phex.xml.XJBMediaType}
     */
    phex.xml.XJBMediaType getMediaType();

    /**
     * Sets the value of the mediaType property.
     * 
     * @param value
     *     allowed object is
     *     {@link phex.xml.XJBMediaType}
     */
    void setMediaType(phex.xml.XJBMediaType value);

    /**
     * Gets the value of the refineText property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getRefineText();

    /**
     * Sets the value of the refineText property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setRefineText(java.lang.String value);

}
