//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.5-20051113-fcs 
// 	See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// 	Any modifications to this file will be lost upon recompilation of the source schema. 
// 	Generated on: 2005.11.16 um 11:03:34 CET 
//


package phex.xml.impl;

public class XJBSharedFileImpl implements phex.xml.XJBSharedFile, com.sun.xml.bind.JAXBObject, phex.xml.impl.runtime.UnmarshallableObject, phex.xml.impl.runtime.XMLSerializable
{

    protected boolean has_LastModified;
    protected long _LastModified;
    protected boolean has_HitCount;
    protected int _HitCount;
    protected java.lang.String _SHA1;
    protected java.lang.String _ThexLowestLevelNodes;
    protected java.lang.String _ThexRootHash;
    protected boolean has_LastSeen;
    protected int _LastSeen;
    protected boolean has_ThexTreeDepth;
    protected int _ThexTreeDepth;
    protected java.lang.String _FileName;
    protected com.sun.xml.bind.util.ListImpl _AltLoc;
    protected boolean has_UploadCount;
    protected int _UploadCount;
    public final static java.lang.Class version = (phex.xml.impl.JAXBVersion.class);

    private final static java.lang.Class PRIMARY_INTERFACE_CLASS() {
        return (phex.xml.XJBSharedFile.class);
    }

    public long getLastModified() {
        return _LastModified;
    }

    public void setLastModified(long value) {
        _LastModified = value;
        has_LastModified = true;
    }

    public int getHitCount() {
        return _HitCount;
    }

    public void setHitCount(int value) {
        _HitCount = value;
        has_HitCount = true;
    }

    public java.lang.String getSHA1() {
        return _SHA1;
    }

    public void setSHA1(java.lang.String value) {
        _SHA1 = value;
    }

    public java.lang.String getThexLowestLevelNodes() {
        return _ThexLowestLevelNodes;
    }

    public void setThexLowestLevelNodes(java.lang.String value) {
        _ThexLowestLevelNodes = value;
    }

    public java.lang.String getThexRootHash() {
        return _ThexRootHash;
    }

    public void setThexRootHash(java.lang.String value) {
        _ThexRootHash = value;
    }

    public int getLastSeen() {
        return _LastSeen;
    }

    public void setLastSeen(int value) {
        _LastSeen = value;
        has_LastSeen = true;
    }

    public int getThexTreeDepth() {
        return _ThexTreeDepth;
    }

    public void setThexTreeDepth(int value) {
        _ThexTreeDepth = value;
        has_ThexTreeDepth = true;
    }

    public java.lang.String getFileName() {
        return _FileName;
    }

    public void setFileName(java.lang.String value) {
        _FileName = value;
    }

    protected com.sun.xml.bind.util.ListImpl _getAltLoc() {
        if (_AltLoc == null) {
            _AltLoc = new com.sun.xml.bind.util.ListImpl(new java.util.ArrayList());
        }
        return _AltLoc;
    }

    public java.util.List getAltLoc() {
        return _getAltLoc();
    }

    public int getUploadCount() {
        return _UploadCount;
    }

    public void setUploadCount(int value) {
        _UploadCount = value;
        has_UploadCount = true;
    }

    public phex.xml.impl.runtime.UnmarshallingEventHandler createUnmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
        return new phex.xml.impl.XJBSharedFileImpl.Unmarshaller(context);
    }

    public void serializeBody(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx9 = 0;
        final int len9 = ((_AltLoc == null)? 0 :_AltLoc.size());
        if (_FileName!= null) {
            context.startElement("", "FID");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _FileName), "FileName");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (_SHA1 != null) {
            context.startElement("", "SHA1");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _SHA1), "SHA1");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (_ThexRootHash!= null) {
            context.startElement("", "TxRH");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _ThexRootHash), "ThexRootHash");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (has_ThexTreeDepth) {
            context.startElement("", "TxD");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(javax.xml.bind.DatatypeConverter.printInt(((int) _ThexTreeDepth)), "ThexTreeDepth");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (_ThexLowestLevelNodes!= null) {
            context.startElement("", "TxLLN");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(((java.lang.String) _ThexLowestLevelNodes), "ThexLowestLevelNodes");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (has_LastModified) {
            context.startElement("", "LM");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(javax.xml.bind.DatatypeConverter.printLong(((long) _LastModified)), "LastModified");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (has_LastSeen) {
            context.startElement("", "LS");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(javax.xml.bind.DatatypeConverter.printInt(((int) _LastSeen)), "LastSeen");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (has_HitCount) {
            context.startElement("", "HC");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(javax.xml.bind.DatatypeConverter.printInt(((int) _HitCount)), "HitCount");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        if (has_UploadCount) {
            context.startElement("", "UC");
            context.endNamespaceDecls();
            context.endAttributes();
            try {
                context.text(javax.xml.bind.DatatypeConverter.printInt(((int) _UploadCount)), "UploadCount");
            } catch (java.lang.Exception e) {
                phex.xml.impl.runtime.Util.handlePrintConversionException(this, e, context);
            }
            context.endElement();
        }
        while (idx9 != len9) {
            context.startElement("", "AltLoc");
            int idx_18 = idx9;
            context.childAsURIs(((com.sun.xml.bind.JAXBObject) _AltLoc.get(idx_18 ++)), "AltLoc");
            context.endNamespaceDecls();
            int idx_19 = idx9;
            context.childAsAttributes(((com.sun.xml.bind.JAXBObject) _AltLoc.get(idx_19 ++)), "AltLoc");
            context.endAttributes();
            context.childAsBody(((com.sun.xml.bind.JAXBObject) _AltLoc.get(idx9 ++)), "AltLoc");
            context.endElement();
        }
    }

    public void serializeAttributes(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx9 = 0;
        final int len9 = ((_AltLoc == null)? 0 :_AltLoc.size());
        while (idx9 != len9) {
            idx9 += 1;
        }
    }

    public void serializeURIs(phex.xml.impl.runtime.XMLSerializer context)
        throws org.xml.sax.SAXException
    {
        int idx9 = 0;
        final int len9 = ((_AltLoc == null)? 0 :_AltLoc.size());
        while (idx9 != len9) {
            idx9 += 1;
        }
    }

    public class Unmarshaller
        extends phex.xml.impl.runtime.AbstractUnmarshallingEventHandlerImpl
    {


        public Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context) {
            super(context, "-------------------------------");
        }

        protected Unmarshaller(phex.xml.impl.runtime.UnmarshallingContext context, int startState) {
            this(context);
            state = startState;
        }

        public java.lang.Object owner() {
            return phex.xml.impl.XJBSharedFileImpl.this;
        }

        public void enterElement(java.lang.String ___uri, java.lang.String ___local, java.lang.String ___qname, org.xml.sax.Attributes __atts)
            throws org.xml.sax.SAXException
        {
            int attIdx;
            outer:
            while (true) {
                switch (state) {
                    case  18 :
                        if (("LS" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 19;
                            return ;
                        }
                        state = 21;
                        continue outer;
                    case  30 :
                        if (("AltLoc" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 28;
                            return ;
                        }
                        revertToParentFromEnterElement(___uri, ___local, ___qname, __atts);
                        return ;
                    case  15 :
                        if (("LM" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 16;
                            return ;
                        }
                        state = 18;
                        continue outer;
                    case  0 :
                        if (("FID" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 1;
                            return ;
                        }
                        state = 3;
                        continue outer;
                    case  3 :
                        if (("SHA1" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 4;
                            return ;
                        }
                        state = 6;
                        continue outer;
                    case  28 :
                        if (("host-address" == ___local)&&("" == ___uri)) {
                            _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromEnterElement((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        if (("URN" == ___local)&&("" == ___uri)) {
                            _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromEnterElement((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname, __atts)));
                            return ;
                        }
                        _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromEnterElement((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname, __atts)));
                        return ;
                    case  21 :
                        if (("HC" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 22;
                            return ;
                        }
                        state = 24;
                        continue outer;
                    case  6 :
                        if (("TxRH" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 7;
                            return ;
                        }
                        state = 9;
                        continue outer;
                    case  12 :
                        if (("TxLLN" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 13;
                            return ;
                        }
                        state = 15;
                        continue outer;
                    case  24 :
                        if (("UC" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 25;
                            return ;
                        }
                        state = 27;
                        continue outer;
                    case  9 :
                        if (("TxD" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, true);
                            state = 10;
                            return ;
                        }
                        state = 12;
                        continue outer;
                    case  27 :
                        if (("AltLoc" == ___local)&&("" == ___uri)) {
                            context.pushAttributes(__atts, false);
                            state = 28;
                            return ;
                        }
                        state = 30;
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
                    case  18 :
                        state = 21;
                        continue outer;
                    case  29 :
                        if (("AltLoc" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 30;
                            return ;
                        }
                        break;
                    case  8 :
                        if (("TxRH" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 9;
                            return ;
                        }
                        break;
                    case  30 :
                        revertToParentFromLeaveElement(___uri, ___local, ___qname);
                        return ;
                    case  15 :
                        state = 18;
                        continue outer;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  14 :
                        if (("TxLLN" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 15;
                            return ;
                        }
                        break;
                    case  3 :
                        state = 6;
                        continue outer;
                    case  23 :
                        if (("HC" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 24;
                            return ;
                        }
                        break;
                    case  28 :
                        _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromLeaveElement((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname)));
                        return ;
                    case  21 :
                        state = 24;
                        continue outer;
                    case  17 :
                        if (("LM" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 18;
                            return ;
                        }
                        break;
                    case  11 :
                        if (("TxD" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 12;
                            return ;
                        }
                        break;
                    case  20 :
                        if (("LS" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 21;
                            return ;
                        }
                        break;
                    case  6 :
                        state = 9;
                        continue outer;
                    case  12 :
                        state = 15;
                        continue outer;
                    case  24 :
                        state = 27;
                        continue outer;
                    case  2 :
                        if (("FID" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 3;
                            return ;
                        }
                        break;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  5 :
                        if (("SHA1" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 6;
                            return ;
                        }
                        break;
                    case  26 :
                        if (("UC" == ___local)&&("" == ___uri)) {
                            context.popAttributes();
                            state = 27;
                            return ;
                        }
                        break;
                    case  27 :
                        state = 30;
                        continue outer;
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
                    case  18 :
                        state = 21;
                        continue outer;
                    case  30 :
                        revertToParentFromEnterAttribute(___uri, ___local, ___qname);
                        return ;
                    case  15 :
                        state = 18;
                        continue outer;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        state = 6;
                        continue outer;
                    case  28 :
                        _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromEnterAttribute((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname)));
                        return ;
                    case  21 :
                        state = 24;
                        continue outer;
                    case  6 :
                        state = 9;
                        continue outer;
                    case  12 :
                        state = 15;
                        continue outer;
                    case  24 :
                        state = 27;
                        continue outer;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  27 :
                        state = 30;
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
                    case  18 :
                        state = 21;
                        continue outer;
                    case  30 :
                        revertToParentFromLeaveAttribute(___uri, ___local, ___qname);
                        return ;
                    case  15 :
                        state = 18;
                        continue outer;
                    case  0 :
                        state = 3;
                        continue outer;
                    case  3 :
                        state = 6;
                        continue outer;
                    case  28 :
                        _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromLeaveAttribute((phex.xml.impl.XJBAlternateLocationImpl.class), 29, ___uri, ___local, ___qname)));
                        return ;
                    case  21 :
                        state = 24;
                        continue outer;
                    case  6 :
                        state = 9;
                        continue outer;
                    case  12 :
                        state = 15;
                        continue outer;
                    case  24 :
                        state = 27;
                        continue outer;
                    case  9 :
                        state = 12;
                        continue outer;
                    case  27 :
                        state = 30;
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
                        case  18 :
                            state = 21;
                            continue outer;
                        case  10 :
                            state = 11;
                            eatText1(value);
                            return ;
                        case  13 :
                            state = 14;
                            eatText2(value);
                            return ;
                        case  30 :
                            revertToParentFromText(value);
                            return ;
                        case  15 :
                            state = 18;
                            continue outer;
                        case  0 :
                            state = 3;
                            continue outer;
                        case  19 :
                            state = 20;
                            eatText3(value);
                            return ;
                        case  3 :
                            state = 6;
                            continue outer;
                        case  22 :
                            state = 23;
                            eatText4(value);
                            return ;
                        case  28 :
                            _getAltLoc().add(((phex.xml.impl.XJBAlternateLocationImpl) spawnChildFromText((phex.xml.impl.XJBAlternateLocationImpl.class), 29, value)));
                            return ;
                        case  21 :
                            state = 24;
                            continue outer;
                        case  6 :
                            state = 9;
                            continue outer;
                        case  12 :
                            state = 15;
                            continue outer;
                        case  25 :
                            state = 26;
                            eatText5(value);
                            return ;
                        case  24 :
                            state = 27;
                            continue outer;
                        case  9 :
                            state = 12;
                            continue outer;
                        case  4 :
                            state = 5;
                            eatText6(value);
                            return ;
                        case  7 :
                            state = 8;
                            eatText7(value);
                            return ;
                        case  16 :
                            state = 17;
                            eatText8(value);
                            return ;
                        case  27 :
                            state = 30;
                            continue outer;
                        case  1 :
                            state = 2;
                            eatText9(value);
                            return ;
                    }
                } catch (java.lang.RuntimeException e) {
                    handleUnexpectedTextException(value, e);
                }
                break;
            }
        }

        private void eatText1(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ThexTreeDepth = javax.xml.bind.DatatypeConverter.parseInt(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_ThexTreeDepth = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText2(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ThexLowestLevelNodes = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText3(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _LastSeen = javax.xml.bind.DatatypeConverter.parseInt(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_LastSeen = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText4(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _HitCount = javax.xml.bind.DatatypeConverter.parseInt(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_HitCount = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText5(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _UploadCount = javax.xml.bind.DatatypeConverter.parseInt(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_UploadCount = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText6(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _SHA1 = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText7(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _ThexRootHash = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText8(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _LastModified = javax.xml.bind.DatatypeConverter.parseLong(com.sun.xml.bind.WhiteSpaceProcessor.collapse(value));
                has_LastModified = true;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

        private void eatText9(final java.lang.String value)
            throws org.xml.sax.SAXException
        {
            try {
                _FileName = value;
            } catch (java.lang.Exception e) {
                handleParseConversionException(e);
            }
        }

    }

}
