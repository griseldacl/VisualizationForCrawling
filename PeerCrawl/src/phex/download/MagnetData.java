/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- CVS Information ---
 *  $Id: MagnetData.java,v 1.7 2005/10/03 00:18:23 gregork Exp $
 */

package phex.download;

import java.util.*;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.URN;
import phex.utils.URLCodecUtils;

/**
 * 
 */

public class MagnetData
{
    /**
     * Meaning a precise description of the topic/resource/file 
     * you want to perform local options on. The 'xt' value is 
     * typically a URI of some sort, perhaps an URN/hash, perhaps an HTTP URL.
     * Also: xt
     */
    private String exactTopic;

    /**
     * Meaning another URI that is asserted to be a perfect 
     * substitute for the 'xt' resource. An HTTP URL could be 
     * provided as an 'xs' for an URN hash name, or vice-versa. 
     * (In such cases, the returned content from the HTTP URL 
     * should match the hash value, at least at the time the link 
     * is first composed -- but software should be ready for the 
     * inevitable mismatches, and in such cases, the 'xt' wins out.)
     * Also: xs
     */

    private String exactSubstitute;

    /**
     * Meaning another URI that can provide a resource essentially as good as the
     * 'xt', but identicality is not implied.
     * Also: as
     */
    private List acceptableSubstituteList;

    /**
     * A convenient human-readable label for the "exact topic" -- 
     * but not in any way a constraining/certain label.
     * Also: dn
     */

    private String displayName;

    private String keywordTopic;
    
    private MagnetData()
    {
        acceptableSubstituteList = new ArrayList();
    }

    /**
     * @return Returns the displayName.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @param displayName The displayName to set.
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * @return Returns the keywordTopic.
     */
    public String getKeywordTopic()
    {
        return keywordTopic;
    }

    /**
     * @param keywordTopic The keywordTopic to set.
     */
    public void setKeywordTopic(String keywordTopic)
    {
        this.keywordTopic = keywordTopic;
    }

    /**
     * @return Returns the exactSubstitute.
     */

    public String getExactSubstitute()
    {
        return exactSubstitute;
    }

    /**
     * @param exactSubstitute The exactSubstitute to set.
     */
    public void setExactSubstitute(String exactSubstitute)
    {
        this.exactSubstitute = exactSubstitute;
    }
    
     /**
      * @return Returns the acceptable substitute list.
      */
    public List getAcceptableSubstituteList()
    {
        return acceptableSubstituteList;
    }

    /**
     * @param acceptableSubstitute The acceptable substitute to add.
     */
    private void addAcceptableSubstitute(String acceptableSubstitutes)
    {
        this.acceptableSubstituteList.add(acceptableSubstitutes);
    }

    /**
     * @return Returns the exactTopic.
     */
    public String getExactTopic()
    {
        return exactTopic;
    }
    
    /**
     * @param exactTopicStr
     */
    public void setExactTopic(String exactTopicStr)
    {
        exactTopic = exactTopicStr;
    }
    
    public static String lookupFileName( MagnetData magnetData )
    {
        if ( magnetData.displayName != null )
        {
            return magnetData.displayName;
        }
        
        if ( magnetData.keywordTopic != null )
        {
            return magnetData.keywordTopic;
        }
        
        URN urn = lookupSHA1URN(magnetData);
        if ( urn != null )
        {
            return "Magnet download " + urn.getAsString();
        }
        
        return "Unknown Magnet download";
    }
    
    public static String lookupSearchName( MagnetData magnetData )
    {
        if ( magnetData.keywordTopic != null )
        {
            return magnetData.keywordTopic;
        }
        
        if ( magnetData.displayName != null )
        {
            return magnetData.displayName;
        }
        
        return "";
    }

    public static URN lookupSHA1URN(MagnetData magnetData)
    {
        if ( magnetData.exactTopic != null 
            && URN.isValidURN(magnetData.exactTopic))
        {
            return new URN(magnetData.exactTopic);
        }
        else if ( magnetData.exactSubstitute != null 
            && URN.isValidURN(magnetData.exactSubstitute))
        {
            return new URN(magnetData.exactSubstitute);
        }
        Iterator asIterator = magnetData.acceptableSubstituteList.iterator();
        while( asIterator.hasNext() )
        {
            String as = (String) asIterator.next();
            if (URN.isValidURN(as))
            {
                return new URN(as);
            } 
        }
        return null;
    }

    public static List lookupHttpURIs(MagnetData magnetData)
    {
        List urlList = new ArrayList();

        if (magnetData.exactTopic != null
            && magnetData.exactTopic.startsWith("http://"))
        {
            try
            {
                urlList.add(new URI(magnetData.exactTopic, false));
            }
            catch (URIException e)
            {
                // ignore try next
            }
        }
        if (magnetData.exactSubstitute != null
            && magnetData.exactSubstitute.startsWith("http://"))
        {
            try
            {
                urlList.add(new URI(magnetData.exactSubstitute, false));
            }
            catch (URIException e)
            {
                // ignore try next
            }
        }
        
        Iterator asIterator = magnetData.acceptableSubstituteList.iterator();
        while( asIterator.hasNext() )
        {
            String as = (String) asIterator.next();
            if ( as.startsWith("http://"))
            {
                try
                {
                    urlList.add(new URI(as, false));
                }
                catch (URIException e)
                {
                    // ignore try next
                }
            }
        }
        return urlList;
    }

    public static MagnetData parseFromURI(URI uri)
    {
        String protocol = uri.getScheme();
        if (!"magnet".equals(protocol))
        {
            return null;
        }

        MagnetData magnetData = new MagnetData();

        String urlQuery = uri.getEscapedQuery();

        StringTokenizer tokenizer = new StringTokenizer(urlQuery, "&");
        while (tokenizer.hasMoreTokens())
        {
            String param = tokenizer.nextToken().trim();
            int seperatorIdx = param.indexOf("=");
            if (seperatorIdx == -1)
            {// no = found.
                continue;
            }
            String key = param.substring(0, seperatorIdx);
            String value = param.substring(seperatorIdx + 1);
            value = URLCodecUtils.decodeURL(value);

            if (key.equals("xt"))
            {
                magnetData.setExactTopic(value);
            }
            else if (key.equals("xs"))
            {
                magnetData.setExactSubstitute(value);
            }
            else if (key.equals("as"))
            {
                magnetData.addAcceptableSubstitute(value);
            }
            else if (key.equals("dn"))
            {
                magnetData.setDisplayName(value);
            }
            else if (key.equals("kt"))
            {
                magnetData.setKeywordTopic(value);
            }
        }
        return magnetData;
    }
}