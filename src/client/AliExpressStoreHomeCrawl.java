package client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.naming.InitialContext;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import util.Configuration;

/*****
 *  
 * 
 */

public class AliExpressStoreHomeCrawl {

	private static WebClient webClient;
	private static String conPath = "/workspace/AliExpressCrawl/configuration.properties";
	private static Logger logger = Logger.getLogger(AliExpressStoreHomeCrawl.class);
	private static String url = "https://valuefashionshop.aliexpress.com/store/all-wholesale-products/1248036.html";
	private static AliExpressExtract pageExtract = new AliExpressExtract();
	private static String storeId;
	private static String pageUrl = "https://valuefashionshop.aliexpress.com/store/";// storeId/search/1.html
	private static int maxPage = 1;
	private static Cookie cookie=null;

	public static void main(String[] args) {
		// 初始化程序,加载配置文件
		initial();
	
		StringBuilder builder = null;
		pageUrl=pageUrl+getStoreId(url)+"/search/";

		builder = crawlPage(url);// 提取最大翻页数目
		if(!pageExtract.loginCheck(builder)){
			System.out.println("请登录!");
			return;
		}
		
		maxPage = pageExtract.extractPage(builder);
		for (int i = 1; i <= maxPage; i++) {// 开始翻页
			String tpageUrl = pageUrl + i + ".html";
			System.out.println("Crawl Page:" + tpageUrl);
			builder = crawlPage(tpageUrl);// 采集具体的页面
			pageExtract.extractProduct(builder);
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private static String getStoreId(String url) {
		String id = null;
		int index = url.indexOf("/all-wholesale-products/");
		if (index != -1) {
			id = url.substring(index + 24, index + 31);
		}
		return id;
	}

	private static void initial() {
		Configuration.loadConfiguration(conPath);
	}

	private static WebClient getAWebClient() {
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_17);
		webClient.getOptions().setTimeout(20000);
		//webClient.getCookieManager().setCookiesEnabled(true);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getOptions().setUseInsecureSSL(true);// 解决ssh证书访问https的问题
		webClient.addRequestHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		webClient.addRequestHeader("Accept-Encoding", "gzip, deflate");
		webClient.addRequestHeader("Accept-Language", "en-US,en;q=0.5");
		webClient.addRequestHeader("Cache-Control", "max-age=0");
		webClient.addRequestHeader("Connection", "keep-alive");
		webClient.addRequestHeader(":authority", "rogesi.aliexpress.com");
		webClient.addRequestHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
		webClient.addRequestHeader(":scheme", "https");
		webClient.addRequestHeader(":method", "GET");
		webClient.addRequestHeader("upgrade-insecure-requests", "1");
		String cookieStr=new String(Configuration.getProperties("cookies_1"));
		String[] strs=cookieStr.split(";");
		for(int i=0;i<strs.length;i++){
			String str=strs[i].replaceAll(" ", "");
			int index=str.indexOf("=");
			String name=str.substring(0, index);
			String value=str.substring(index+1);
			Date date= null;
			Cookie cookie = new Cookie("",name, value, "/",date, false);
			System.out.println(cookie);
			//webClient.getCookieManager().addCookie(cookie);
		}
		return webClient;
	}

	/**
	 * 采集网页
	 */
	public static StringBuilder crawlPage(String url) {
		webClient = getAWebClient();
		StringBuilder builder = new StringBuilder();
		HtmlPage page = null;
		try {
			page = webClient.getPage(url);
		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		builder.append(page.asXml());
		webClient.closeAllWindows();
		return builder;
	}
}
