//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBSWDownloadListImpl implements phex.xml.XJBSWDownloadList, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected com.sun.xml.bind.util.ListImpl _SWDownloadFileList;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBSWDownloadList.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getSWDownloadFileList() {
        if (_SWDownloadFileList == null) {
            _SWDownloadFileList = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _SWDownloadFileList;
    }

    public java.util.List getSWDownloadFileList() {
        return _getSWDownloadFileList();
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBSWDownloadListImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SWDownloadFileList == null)? 0 :_SWDownloadFileList.size());
        while (idx1 != len1) {
            context.startElement("", "swDownloadFile");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _SWDownloadFileList.get(idx_0 ++)), "SWDownloadFileList");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _SWDownloadFileList.get(idx_1 ++)), "SWDownloadFileList");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _SWDownloadFileList.get(idx1 ++)), "SWDownloadFileList");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SWDownloadFileList == null)? 0 :_SWDownloadFileList.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_SWDownloadFileList == null)? 0 :_SWDownloadFileList.size());
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
            return phex.xml.impl.XJBSWDownloadListImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  3 :
                        if (("swDownloadFile" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  1 :
                        if (("localfilename" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("incomplete-file-name" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("searchterm" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("filesize" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("file-urn" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("scope-strategy" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("status" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("created-time" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("modified-time" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("candidate" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("finished-scopes" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("segment" == ___local)&&("" == ___uri)) {
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                        return ;
                    case  0 :
                        if (("swDownloadFile" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
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
                    case  1 :
                        _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  2 :
                        if (("swDownloadFile" == ___local)&&("" == ___uri)) {
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
                    case  1 :
                        _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
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
                    case  1 :
                        _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, ___uri, ___local, ___qname)));
                        return ;
                    case  0 :
                        state = 3;
                        continue outer;
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
                        case  1 :
                            _getSWDownloadFileList().add(((phex.xml.impl.XJBSWDownloadFileImpl) spawnChildFromText((phex.xml.impl.XJBSWDownloadFileImpl.class), 2, value)));
                            return ;
                        case  0 :
                            state = 3;
                            continue outer;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

    }

}