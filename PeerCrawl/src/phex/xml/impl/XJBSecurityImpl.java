//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBSecurityImpl implements phex.xml.XJBSecurity, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected com.sun.xml.bind.util.ListImpl _IpAccessRuleList;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBSecurity.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getIpAccessRuleList() {
        if (_IpAccessRuleList == null) {
            _IpAccessRuleList = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _IpAccessRuleList;
    }

    public java.util.List getIpAccessRuleList() {
        return _getIpAccessRuleList();
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBSecurityImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_IpAccessRuleList == null)? 0 :_IpAccessRuleList.size());
        while (idx1 != len1) {
            context.startElement("", "ip-access-rule");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _IpAccessRuleList.get(idx_0 ++)), "IpAccessRuleList");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _IpAccessRuleList.get(idx_1 ++)), "IpAccessRuleList");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _IpAccessRuleList.get(idx1 ++)), "IpAccessRuleList");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_IpAccessRuleList == null)? 0 :_IpAccessRuleList.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_IpAccessRuleList == null)? 0 :_IpAccessRuleList.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public class Unmarshaller
        extends phex.xml.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
            super(context, "----");
        }

        protected Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return phex.xml.impl.XJBSecurityImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        if (("ip-access-rule" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  0 :
                        if (("ip-access-rule" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  1 :
                        if (("description" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("isDenyingRule" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("isDisabled" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("isSystemRule" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("triggerCount" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("expiryDate" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("isDeletedOnExpiry" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("addressType" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("ip" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("compareIP" == ___local)&&("" == ___uri)) {
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                        return ;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  1 :
                        _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                    case  2 :
                        if (("ip-access-rule" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                }
                super.leaveElement(___uri, ___local, ___qname);
                break;
            }
        }

        public void enterAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  1 :
                        _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                }
                super.enterAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void leaveAttribute(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  1 :
                        _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                }
                super.leaveAttribute(___uri, ___local, ___qname);
                break;
            }
        }

        public void handleText(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                try {
                    switch (state) {
                        case  3 :
                            revertToParentFromText(value);
                            return ;
                        case  0 :
                            state = 3;
                            continue outer;
                        case  1 :
                            _getIpAccessRuleList().add(((phex.xml.impl.XJBIPAccessRuleImpl) spawnChildFromText((phex.xml.impl.XJBIPAccessRuleImpl.class), 2, value)));
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

    }

}
