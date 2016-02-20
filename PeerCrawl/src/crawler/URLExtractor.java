/**
 * PeerCrawl - Distributed P2P web crawler based on Gnutella Protocol
 * @version 5.0
 * 
 * Developed as part of Masters Project - Spring 2006
 * @author 	Vaibhav Padliya
 * 			College of Computing
 * 			Georgia Tech
 * 
 * @contributor Mudhakar Srivatsa
 * @contributor Mahesh Palekar
 * @contributor Tushar Bansal
 * @contributor Suiyang Li
 */

package crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import crawler.util.Const;
import crawler.util.Document;
import crawler.util.PCHTMLDocument;
import crawler.util.PCPDFDocument;
import crawler.util.UrlWithDepth;

/**
 * Class URLExtractor : Uses some functions from Heretrix (www.crawler.org) 
 * 							to extract URLs from documents
 * @author Tushar
 */

public class URLExtractor 
{
	private static final int MAX_ATTR_NAME_LENGTH = 1024;
	private static final int MAX_ATTR_VAL_LENGTH = 16384;

	// Template for any url pattern
	static final String EACH_ATTRIBUTE_EXTRACTOR =
		"(?is)\\s((href)|(action)|(on\\w*)" // 1, 2, 3, 4 
		+"|((?:src)|(?:lowsrc)|(?:background)|(?:cite)|(?:longdesc)" // ...
		+"|(?:usemap)|(?:profile)|(?:datasrc))" // 5
		+"|(codebase)|((?:classid)|(?:data))|(archive)|(code)" // 6, 7, 8, 9
		+"|(value)|(style)|([-\\w]{1,"+URLExtractor.MAX_ATTR_NAME_LENGTH+"}))" // 10, 11, 12
		+"\\s*=\\s*"
		+"(?:(?:\"(.{0,"+URLExtractor.MAX_ATTR_VAL_LENGTH+"}?)(?:\"|$))" // 13
		+"|(?:'(.{0,"+URLExtractor.MAX_ATTR_VAL_LENGTH+"}?)(?:'|$))" // 14
		+"|(\\S{1,"+URLExtractor.MAX_ATTR_VAL_LENGTH+"}))"; // 15
	// groups:
	// 1: attribute name
	// 2: HREF - single URI relative to doc base, or occasionally javascript:
	// 3: ACTION - ingle URI relative to doc base, or occasionally javascript:
	// 4: ON[WHATEVER] - script handler
	// 5: SRC,LOWSRC,BACKGROUND,CITE,LONGDESC,USEMAP,PROFILE, or DATASRC
	//    single URI relative to doc base
	// 6: CODEBASE - a single URI relative to doc base, affecting other
	//    attributes
	// 7: CLASSID, DATA - a single URI relative to CODEBASE (if supplied)
	// 8: ARCHIVE - one or more space-delimited URIs relative to CODEBASE
	//    (if supplied)
	// 9: CODE - a single URI relative to the CODEBASE (is specified).
	// 10: VALUE - often includes a uri path on forms
	// 11: STYLE - inline attribute style info
	// 12: any other attribute
	// 13: double-quote delimited attr value
	// 14: single-quote delimited attr value
	// 15: space-delimited attr value


	/**
	 * Extracting urls from HTML. Uses EACH_ATTRIBUTE_EXTRACTOR as a pattern of general urls
	 * @param doc
	 * @return list of urls
	 */
	public static List<UrlWithDepth> extractURLsFromHTML(final PCHTMLDocument doc)
	{
		final List<UrlWithDepth> extractedUrls = new ArrayList<UrlWithDepth>();
		
		
		Matcher attr = null;

		final UrlWithDepth url = doc.getUrl();
		final String pageContent = doc.getDataBuffer().toString();
		attr =  Pattern.compile( URLExtractor.EACH_ATTRIBUTE_EXTRACTOR, 
					Pattern.CASE_INSENSITIVE).matcher(pageContent);

		// If found atleast one url
		while (attr.find()) 
		{
			final int valueGroup =
				(attr.start(13) > -1) ? 13 : (attr.start(14) > -1) ? 14 : 15;
			final int start = attr.start(valueGroup);
			final int end = attr.end(valueGroup);
			String value = pageContent.substring(start, end);
			String temp = null;
			
			if ((attr.start(2) > -1) && !value.startsWith("javascript")
					&& !value.startsWith("file")) 
			{
				if( URLExtractor.isHtmlExpectedHere(value) )
				{
					URL urlTemp = null;
					try 
					{
						// Normalizing url from relative to absolute
						urlTemp = new URL(url.getUrl());
						temp = new URL(urlTemp, value).toExternalForm();
					} catch (MalformedURLException e) { }
				}
				if(temp == null)
					continue;
				
				final String addUrl = URLExtractor.cleanURL(temp);  
				
				// Dont crawl wiki pages
				if((addUrl != null) && (addUrl.indexOf("wiki.") < 0)
						/*&& (addUrl.indexOf("coweb.") < 0)*/ )
					extractedUrls.add(new UrlWithDepth(addUrl, url.getDepth() + 1));
			}
		}

		return extractedUrls;
	} // End of function extractURLsFromHTML()

	
	/**
	 * Extracting urls from PDF
	 * @param doc
	 * @return list of urls
	 */
	public static List<UrlWithDepth> extractURLsFromPDF(final PCPDFDocument doc)
	{
		// Parse with PDF parser
		final List<UrlWithDepth> extractedUrls = new ArrayList<UrlWithDepth>();
		final UrlWithDepth url = doc.getUrl();
		PDFParser parser = null;
		
		try{
			parser = new PDFParser(doc.getByteBuffer());
			ArrayList uris = parser.extractURIs();
			
            for (Iterator itr = uris.iterator(); itr.hasNext(); )
            {
                String uri = (String)itr.next();
                
                final String addUrl = URLExtractor.cleanURL(uri);  
				if((addUrl != null) && (addUrl.indexOf("wiki.") < 0)
						/*&& (addUrl.indexOf("coweb.") < 0)*/ )
					extractedUrls.add(new UrlWithDepth(addUrl, url.getDepth() + 1));
            }
		}catch(IOException io){
			System.out.println("Can not create PDF parser");
			io.printStackTrace();
		}
		
		return extractedUrls;
	}

	
    /**
     * Used to extract urls from document
     * uses different method based on type of document
     * @param doc
     * @return list of urls
     */
	public static List<UrlWithDepth> extractURLs(Document doc)
	{
		if(doc instanceof PCHTMLDocument)
			return URLExtractor.extractURLsFromHTML((PCHTMLDocument) doc);
		else if (doc instanceof PCPDFDocument)
			return URLExtractor.extractURLsFromPDF((PCPDFDocument) doc);
		
		return new ArrayList<UrlWithDepth>();
	}

	
	static final String NON_HTML_PATH_EXTENSION_EXCEPT_PDF =
		"(?i)((gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
		"|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)|(ico)|(css)|(zip)" + 
		"|(txt)|(doc)|(ppt)|(ps)|(gz)|(xls)|(java)|(jar)|(exe))(.*)";

	
	static final String NON_HTML_PATH_EXTENSION =
		"(?i)((gif)|(jp(e)?g)|(png)|(tif(f)?)|(bmp)|(avi)|(mov)|(mp(e)?g)"+
		"|(mp3)|(mp4)|(swf)|(wav)|(au)|(aiff)|(mid)|(ico)|(css)|(zip)" + 
		"|(txt)|(doc)|(ppt)|(ps)|(gz)|(xls)|(java)|(jar)|(exe)|(pdf))(.*)";
	
	
	/**
	 * Test whether this HTML is so unexpected (eg in place of a GIF URI)
	 * that it shouldn't be scanned for links.
	 *
	 * @param path Url to examine
	 * @return True if HTML is acceptable/expected here
	 */
	private static boolean isHtmlExpectedHere(final String path) 
	{
		if(path==null) {
			// no path extension, HTML is fine
			return true;
		}
		final int dot = path.lastIndexOf('.');
		if (dot < 0) {
			// no path extension, HTML is fine
			return true;
		}

		final String ext = path.substring(dot+1);
		if(Const.crawlPDF)
			return ! Pattern.matches(URLExtractor.NON_HTML_PATH_EXTENSION_EXCEPT_PDF, ext);
		
		return ! Pattern.matches(URLExtractor.NON_HTML_PATH_EXTENSION, ext);
	}
	

	/**
	 * Cleans URL
	 * @param url
	 * @return cleaned url
	 */
	public static String cleanURL(String url) 
	{
		String returnUrl = url;

		// Check if url is of protocol ftp, https , mailto, or just url parameters. 
		// If it is, return null.
		if ( (url.indexOf('#') != -1) || (url.indexOf("ftp:") != -1) || 
				(url.indexOf("https:") != -1) || (url.indexOf("mailto:") != -1) || 
				(url.indexOf("?") != -1) || url.equals("") )
			returnUrl = null; 
		else
		{
			// Parse url to add "/" to end of url if no explicit file.
			// e.g. www.cc.gatech.edu/classes ==> www.cc.gatech.edu/classes/ 
			int index = returnUrl.lastIndexOf("/") + 1;
			if( index < returnUrl.length() )
			{
				String tempStr = returnUrl.substring(index);
				if (tempStr.indexOf(".") == -1 && !tempStr.endsWith("/"))
					returnUrl = returnUrl + "/";
			}
		}
		return returnUrl;
	}
	
} // End of Class URLExtractor
