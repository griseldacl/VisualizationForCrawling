//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBGUITableColumnListImpl implements phex.xml.XJBGUITableColumnList, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected com.sun.xml.bind.util.ListImpl _TableColumnList;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBGUITableColumnList.class);
    }

    protected com.sun.xml.bind.util.ListImpl _getTableColumnList() {
        if (_TableColumnList == null) {
            _TableColumnList = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _TableColumnList;
    }

    public java.util.List getTableColumnList() {
        return _getTableColumnList();
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBGUITableColumnListImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_TableColumnList == null)? 0 :_TableColumnList.size());
        while (idx1 != len1) {
            context.startElement("", "table-column");
            int idx_0 = idx1;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _TableColumnList.get(idx_0 ++)), "TableColumnList");
            context.endNamespaceDecls();
            int idx_1 = idx1;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _TableColumnList.get(idx_1 ++)), "TableColumnList");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _TableColumnList.get(idx1 ++)), "TableColumnList");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_TableColumnList == null)? 0 :_TableColumnList.size());
        while (idx1 != len1) {
            idx1 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx1 = 0;
        final int len1 = ((_TableColumnList == null)? 0 :_TableColumnList.size());
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
            return phex.xml.impl.XJBGUITableColumnListImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  0 :
                        if (("table-column" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        if (("table-column" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 1;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  1 :
                        if (("columnID" == ___local)&&("" == ___uri)) {
                            _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("isVisible" == ___local)&&("" == ___uri)) {
                            _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("visibleIndex" == ___local)&&("" == ___uri)) {
                            _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("width" == ___local)&&("" == ___uri)) {
                            _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname, __atts)));
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
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  2 :
                        if (("table-column" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  1 :
                        _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname)));
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
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname)));
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
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  1 :
                        _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBGUITableColumnImpl.class), 2, ___uri, ___local, ___qname)));
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
                        case  0 :
                            state = 3;
                            continue outer;
                        case  3 :
                            revertToParentFromText(value);
                            return ;
                        case  1 :
                            _getTableColumnList().add(((phex.xml.impl.XJBGUITableColumnImpl) spawnChildFromText((phex.xml.impl.XJBGUITableColumnImpl.class), 2, value)));
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