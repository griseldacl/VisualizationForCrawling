//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBSharedLibraryImpl implements phex.xml.XJBSharedLibrary, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected com.sun.xml.bind.util.ListImpl _SharedFileList;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBSharedLibrary.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getSharedFileList() {
        if (_SharedFileList == null) {
            _SharedFileList = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _SharedFileList;
    }

    public java.util.List getSharedFileList() {
        return _getSharedFileList();
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBSharedLibraryImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SharedFileList == null)? 0 :_SharedFileList.size());
        while (idx1 != len1) {
            context.startElement("", "SF");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _SharedFileList.get(idx_0 ++)), "SharedFileList");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _SharedFileList.get(idx_1 ++)), "SharedFileList");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _SharedFileList.get(idx1 ++)), "SharedFileList");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SharedFileList == null)? 0 :_SharedFileList.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SharedFileList == null)? 0 :_SharedFileList.size());
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
            return phex.xml.impl.XJBSharedLibraryImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        if (("SF" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  0 :
                        if (("SF" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  1 :
                        if (("FID" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("SHA1" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("TxRH" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("TxD" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("TxLLN" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("LM" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("LS" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("HC" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("UC" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("AltLoc" == ___local)&&("" == ___uri)) {
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
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
                    case  2 :
                        if (("SF" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  1 :
                        _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
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
                        _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname)));
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
                        _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBSharedFileImpl.class), 2, ___uri, ___local, ___qname)));
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
                            _getSharedFileList().add(((phex.xml.impl.XJBSharedFileImpl) spawnChildFromText((phex.xml.impl.XJBSharedFileImpl.class), 2, value)));
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