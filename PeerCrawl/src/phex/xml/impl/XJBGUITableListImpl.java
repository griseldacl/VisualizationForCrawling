//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBGUITableListImpl implements phex.xml.XJBGUITableList, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected boolean has_ShowHorizontalLines;
    protected boolean _ShowHorizontalLines;
    protected com.sun.xml.bind.util.ListImpl _TableList;
    protected boolean has_ShowVerticalLines;
    protected boolean _ShowVerticalLines;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBGUITableList.class);
    }

    public boolean isShowHorizontalLines() {
        return _ShowHorizontalLines;
    }

    public void setShowHorizontalLines(boolean value) {
        _ShowHorizontalLines = value;
        has_ShowHorizontalLines = true;
    }

    public boolean isSetShowHorizontalLines() {
        return has_ShowHorizontalLines;
    }

    public void unsetShowHorizontalLines() {
        has_ShowHorizontalLines = false;
    }

    protected com.sun.xml.bind.util.ListImpl _getTableList() {
        if (_TableList == null) {
            _TableList = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _TableList;
    }

    public java.util.List getTableList() {
        return _getTableList();
    }

    public boolean isShowVerticalLines() {
        return _ShowVerticalLines;
    }

    public void setShowVerticalLines(boolean value) {
        _ShowVerticalLines = value;
        has_ShowVerticalLines = true;
    }

    public boolean isSetShowVerticalLines() {
        return has_ShowVerticalLines;
    }

    public void unsetShowVerticalLines() {
        has_ShowVerticalLines = false;
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBGUITableListImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_TableList == null)? 0 :_TableList.size());
        while (idx2 != len2) {
            context.startElement("", "table");
            int idx_0 = idx2;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _TableList.get(idx_0 ++)), "TableList");
            context.endNamespaceDecls();
            int idx_1 = idx2;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _TableList.get(idx_1 ++)), "TableList");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _TableList.get(idx2 ++)), "TableList");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_TableList == null)? 0 :_TableList.size());
        if (has_ShowHorizontalLines) {
            context.startAttribute("", "showHorizontalLines");
            try {
                context.text(javax.xml.bind.DatatypeConverter.printBoolean(((boolean) _ShowHorizontalLines)), "ShowHorizontalLines");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
        if (has_ShowVerticalLines) {
            context.startAttribute("", "showVerticalLines");
            try {
                context.text(javax.xml.bind.DatatypeConverter.printBoolean(((boolean) _ShowVerticalLines)), "ShowVerticalLines");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endAttribute();
        }
        while (idx2 != len2) {
            idx2 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx2 = 0;
        final int len2 = ((_TableList == null)? 0 :_TableList.size());
        while (idx2 != len2) {
            idx2 += 1;
        }
    }

    public class Unmarshaller
        extends phex.xml.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
            super(context, "----------");
        }

        protected Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return phex.xml.impl.XJBGUITableListImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  6 :
                        if (("table" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 7;
                            return ;
                        }
                        state = 9;
                        continue outer;
                    case  7 :
                        if (("tableIdentifier" == ___local)&&("" == ___uri)) {
                            _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("column-list" == ___local)&&("" == ___uri)) {
                            _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromEnterElement((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname, __atts)));
                        return ;
                    case  3 :
                        attIdx = context.getAttribute("", "showVerticalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText1(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  0 :
                        attIdx = context.getAttribute("", "showHorizontalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText2(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  9 :
                        if (("table" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 7;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                }
                super.enterElement(___uri, ___local, ___qname, __atts);
                break;
            }
        }

        private void eatText1(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ShowVerticalLines = javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_ShowVerticalLines = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ShowHorizontalLines = javax.xml.bind.DatatypeConverter.parseBoolean(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_ShowHorizontalLines = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        public void leaveElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  6 :
                        state = 9;
                        continue outer;
                    case  7 :
                        _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname)));
                        return ;
                    case  3 :
                        attIdx = context.getAttribute("", "showVerticalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText1(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  0 :
                        attIdx = context.getAttribute("", "showHorizontalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText2(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  9 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  8 :
                        if (("table" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 9;
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
                    case  6 :
                        state = 9;
                        continue outer;
                    case  7 :
                        _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname)));
                        return ;
                    case  3 :
                        if (("showVerticalLines" == ___local)&&("" == ___uri)) {
                            state = 4;
                            return ;
                        }
                        state = 6;
                        continue outer;
                    case  0 :
                        if (("showHorizontalLines" == ___local)&&("" == ___uri)) {
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  9 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
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
                    case  5 :
                        if (("showVerticalLines" == ___local)&&("" == ___uri)) {
                            state = 6;
                            return ;
                        }
                        break;
                    case  6 :
                        state = 9;
                        continue outer;
                    case  7 :
                        _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBGUITableImpl.class), 8, ___uri, ___local, ___qname)));
                        return ;
                    case  3 :
                        attIdx = context.getAttribute("", "showVerticalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 6;
                            eatText1(v);
                            continue outer;
                        }
                        state = 6;
                        continue outer;
                    case  0 :
                        attIdx = context.getAttribute("", "showHorizontalLines");
                        if (attIdx >= 0) {
                            final java.lang.String v = context.eatAttribute(attIdx);
                            state = 3;
                            eatText2(v);
                            continue outer;
                        }
                        state = 3;
                        continue outer;
                    case  9 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  2 :
                        if (("showHorizontalLines" == ___local)&&("" == ___uri)) {
                            state = 3;
                            return ;
                        }
                        break;
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
                        case  4 :
                            state = 5;
                            eatText1(value);
                            return ;
                        case  1 :
                            state = 2;
                            eatText2(value);
                            return ;
                        case  6 :
                            state = 9;
                            continue outer;
                        case  7 :
                            _getTableList().add(((phex.xml.impl.XJBGUITableImpl) spawnChildFromText((phex.xml.impl.XJBGUITableImpl.class), 8, value)));
                            return ;
                        case  3 :
                            attIdx = context.getAttribute("", "showVerticalLines");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 6;
                                eatText1(v);
                                continue outer;
                            }
                            state = 6;
                            continue outer;
                        case  0 :
                            attIdx = context.getAttribute("", "showHorizontalLines");
                            if (attIdx >= 0) {
                                final java.lang.String v = context.eatAttribute(attIdx);
                                state = 3;
                                eatText2(v);
                                continue outer;
                            }
                            state = 3;
                            continue outer;
                        case  9 :
                            revertToParentFromText(value);
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
