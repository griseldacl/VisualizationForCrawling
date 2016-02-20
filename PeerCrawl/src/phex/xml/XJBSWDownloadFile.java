//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml;


/**
 * Java content class for XJBSWDownloadFile complex type.
 * 	<p>The following schema fragment specifies the expected 	content contained within this java content object. 	(defined at file:/C:/projects/sourceforge/phex-project/trunk/phex/src/phex/xml/phex.xsd line 359)
 * <p>
 * <pre>
 * &lt;complexType name="XJBSWDownloadFile">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="localfilename" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="incomplete-file-name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="searchterm" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="filesize" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="file-urn" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="scope-strategy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}short" minOccurs="0"/>
 *         &lt;element name="created-time" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="modified-time" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/>
 *         &lt;element name="candidate" type="{}XJBSWDownloadCandidate" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="finished-scopes" type="{}XJBDownloadScope" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="segment" type="{}XJBSWDownloadSegment" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 */
public interface XJBSWDownloadFile {


    /**
     * Gets the value of the status property.
     * 
     */
    short getStatus();

    /**
     * Sets the value of the status property.
     * 
     */
    void setStatus(short value);

    /**
     * Gets the value of the CandidateList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the CandidateList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCandidateList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link phex.xml.XJBSWDownloadCandidate}
     * 
     */
    java.util.List getCandidateList();

    /**
     * Gets the value of the modifiedTime property.
     * 
     */
    long getModifiedTime();

    /**
     * Sets the value of the modifiedTime property.
     * 
     */
    void setModifiedTime(long value);

    /**
     * Gets the value of the searchTerm property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getSearchTerm();

    /**
     * Sets the value of the searchTerm property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setSearchTerm(java.lang.String value);

    /**
     * Gets the value of the fileURN property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getFileURN();

    /**
     * Sets the value of the fileURN property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setFileURN(java.lang.String value);

    /**
     * Gets the value of the FinishedScopesList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the FinishedScopesList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFinishedScopesList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link phex.xml.XJBDownloadScope}
     * 
     */
    java.util.List getFinishedScopesList();

    /**
     * Gets the value of the incompleteFileName property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getIncompleteFileName();

    /**
     * Sets the value of the incompleteFileName property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setIncompleteFileName(java.lang.String value);

    /**
     * Gets the value of the scopeSelectionStrategy property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getScopeSelectionStrategy();

    /**
     * Sets the value of the scopeSelectionStrategy property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setScopeSelectionStrategy(java.lang.String value);

    /**
     * Gets the value of the localFileName property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String}
     */
    java.lang.String getLocalFileName();

    /**
     * Sets the value of the localFileName property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String}
     */
    void setLocalFileName(java.lang.String value);

    /**
     * Gets the value of the SegmentList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the SegmentList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSegmentList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link phex.xml.XJBSWDownloadSegment}
     * 
     */
    java.util.List getSegmentList();

    /**
     * Gets the value of the createdTime property.
     * 
     */
    long getCreatedTime();

    /**
     * Sets the value of the createdTime property.
     * 
     */
    void setCreatedTime(long value);

    /**
     * Gets the value of the fileSize property.
     * 
     */
    long getFileSize();

    /**
     * Sets the value of the fileSize property.
     * 
     */
    void setFileSize(long value);

}
