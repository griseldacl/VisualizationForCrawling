//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml;


/**
 * Java content class for XJBGUITableColumn complex type.
 * 	<p>The following schema fragment specifies the expected 	content contained within this java content object. 	(defined at file:/C:/projects/sourceforge/phex-project/trunk/phex/src/phex/xml/phex.xsd line 261)
 * <p>
 * <pre>
 * &lt;complexType name="XJBGUITableColumn">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="columnID" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="isVisible" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="visibleIndex" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="width" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface XJBGUITableColumn {


    /**
     * Gets the value of the width property.
     * 
     */
    int getWidth();

    /**
     * Sets the value of the width property.
     * 
     */
    void setWidth(int value);

    /**
     * Gets the value of the visible property.
     * 
     */
    boolean isVisible();

    /**
     * Sets the value of the visible property.
     * 
     */
    void setVisible(boolean value);

    /**
     * Gets the value of the visibleIndex property.
     * 
     */
    int getVisibleIndex();

    /**
     * Sets the value of the visibleIndex property.
     * 
     */
    void setVisibleIndex(int value);

    /**
     * Gets the value of the columnID property.
     * 
     */
    int getColumnID();

    /**
     * Sets the value of the columnID property.
     * 
     */
    void setColumnID(int value);

}
