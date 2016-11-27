package weibocrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.http.client.CookieStore;
import org.apache.log4j.Logger;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import util.Configuration;
import util.DBConnection;
import util.FileWriteUtil;
import weiboextractor.WeiboExtrator;

/***
 * 
 * @ClassName: CrawlWeiboComment
 * @Description: 微博评论采集
 * @author zeze
 * @date 2016年11月26日 下午8:16:39
 *
 */
public class CrawlWeiboComment {

	private static Logger logger = Logger.getLogger(CrawlWeiboComment.class);
	private static String conPath = "/workspace/AliExpressCrawl/configuration.properties";
	private static String cookiePath;// cookie目录
	private static int cookiesNum;// cookies数目
	private static String savePath;// 文件保存目录
	private static int sleepTime;// 文件保存目录
	private static String dbName;// 数据库名
	private static String dbUser;// 数据库用户名
	private static String dbPwd;// 数据库密码

	private static String Url = "http://weibo.com/1713926427/Ejoenz7hF?from=page_1005051713926427_profile&wvr=6&mod=weibotime&type=comment#_rnd1480178620097";

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

	public static void main(String[] args) {
		
		if(args.length == 1) {
			conPath=args[0];
		}
		logger.info("Config Path:"+conPath);
		
		while(true){
			// 加载配置文件
			loadConfig();
			Url=sqlList();
			if(Url!=null)
				startCrawl(Url);
			else
				sleepTime(50000);
			sleepTime(10000);
			System.out.println("等待任务10s...");
		}
	}

	private static void startCrawl(String Url) {
		String fromMid = GetMid(Url);// D8hxnrQdM
		String url = "http://weibo.cn/comment/hot/" + fromMid + "?rl=1";
		logger.info("Start:" + url);

		String outputpath = savePath + "/" + fromMid;// 文件保存路径
		String destfile = outputpath + "/" + fromMid + "_Comment.txt";

		File file = new File(outputpath);// 删除原文件
		if (file.exists())
			deleteDir(file);
		if (!file.exists())
			file.mkdirs();

		String fileName = null;// 保存的文件名
		WebClient webClient = null;
		StringBuilder html = null;
		String crawlUrl = null;

		webClient = new WebClient();
		WeiboExtrator extrator = new WeiboExtrator();

		html = crawlPage(url, webClient);

		// 获取热门评论的页数
		int pageNum = extrator.weiboExtratorPageNum(html);
		// 抽取热门评论
		Set<String> commentList = extrator.weiboExtratorCommentList(html);
		// 保存抽取内容
		saveCommentList(commentList, destfile);
		// 插入数据库
		insertCommentToMySql(commentList, fromMid);
		// 保存第一页
		fileName = dateFormat.format(new Date()) + "_Page_1.html";
		savePage(html, outputpath, fileName);
		System.out.println("热门评论的页数：" + pageNum);
		System.out.println("保存成功:" + fileName);

		// 从第二页开始翻页
		for (int i = 2; i <= pageNum; i++) {
			webClient = new WebClient();
			crawlUrl = url + "&page=" + i;
//			logger.info("crawl Page:" + crawlUrl);
			html = crawlPage(crawlUrl, webClient);
			// 抽取热门评论
			commentList = extrator.weiboExtratorCommentList(html);
			// 保存抽取内容
			saveCommentList(commentList, destfile);
			// 插入数据库
			insertCommentToMySql(commentList, fromMid);
			// 保存文件
			fileName = dateFormat.format(new Date()) + "_Page_" + i + ".html";
			savePage(html, outputpath, fileName);// 保存文件
			System.out.println("保存成功:" + fileName);
			sleepTime(sleepTime);
		}
		logger.info("完成采集");
		System.out.println("完成采集");
	}

	// 保存评论内容
	private static void saveCommentList(Set<String> commentList, String destfile) {
		Iterator<String> it = commentList.iterator();
		StringBuffer sBuilder = new StringBuffer();
		while (it.hasNext()) {
			String str = it.next();
			// System.out.println(str);
			sBuilder.append(str + "\r\n");
		}
		FileWriteUtil.WriteDocument(destfile, sBuilder.toString());
	}

	// addBatch批量插入数据库
	public static void insertCommentToMySql(Set<String> commentList, String fromMid) {

		Iterator<String> it = commentList.iterator();

		Statement st = null;
		try {
			st = DBConnection.getConnection(dbName, dbUser, dbPwd).createStatement();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		};

		String sql = null;
		String[] strs = null;
		while (it.hasNext()) {
			strs = it.next().split("\\|");
			// 0zid,1zname,2zmid,3ztext,4zsource,5zzan,6ztime
			sql = "insert into weibo_comment(uid,name,mid,text,source,zan,time,fromMid) values ('" + strs[0] + "','"
					+ strs[1] + "','" + strs[2] + "','" + strs[3] + "','" + strs[4] + "','" + strs[5] + "','" + strs[6]
					+ "','" + fromMid + "') ON DUPLICATE KEY UPDATE updatetime=NOW(),zan='" + strs[5] + "',time='"
					+ strs[6] + "'";
			try {
				st.addBatch(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			st.executeBatch();
			st.clearBatch();
			st.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.error(e);
		}

	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();// 递归删除目录中的子目录下
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// 目录此时为空，可以删除
		return dir.delete();
	}

	// 采集睡眠时间
	private static void sleepTime(int i) {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 采集网页
	 */
	public static StringBuilder crawlPage(String url, WebClient webClient) {
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

	private static WebClient getAWebClient() {
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_17);

		webClient.getOptions().setTimeout(20000);
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);

		webClient.addRequestHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		webClient.addRequestHeader("Accept-Encoding", "gzip, deflate");
		webClient.addRequestHeader("Accept-Language", "en-US,en;q=0.5");
		webClient.addRequestHeader("Cache-Control", "max-age=0");
		webClient.addRequestHeader("Connection", "keep-alive");
		webClient.addRequestHeader("Host", "weibo.cn");
		webClient.addRequestHeader("upgrade-insecure-requests", "1");
		webClient.addRequestHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36");

		webClient.getCookieManager().setCookiesEnabled(true);
		try {
			// 获取cookies
			CookieStore cookieStore = GetCookieStore();
			List<org.apache.http.cookie.Cookie> l = cookieStore.getCookies();
			for (org.apache.http.cookie.Cookie temp : l) {
				Cookie cookie = new Cookie(temp.getDomain(), temp.getName(), temp.getValue(), temp.getPath(),
						temp.getExpiryDate(), false);
				webClient.getCookieManager().addCookie(cookie);
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(e);
		}

		return webClient;
	}

	// 随机获取cookies
	private static CookieStore GetCookieStore() {
		CookieStore cookieStore = null;
		File file = new File(cookiePathAppendRandom());
		if (file.exists()) {
			FileInputStream fin = null;
			try {
				fin = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			ObjectInputStream in;
			try {
				in = new ObjectInputStream(fin);
				cookieStore = (CookieStore) in.readObject();
				in.close();
			} catch (IOException e) {
				logger.error(e);
			} catch (ClassNotFoundException e) {
				logger.error(e);
			}
		} else {
			logger.error("CookiePath doesn`t exit !!!");
		}
		return cookieStore;
	}

	// 随机获取cookie值
	private static String cookiePathAppendRandom() {
		Random random = new Random();
		int index = random.nextInt(cookiesNum);
		logger.info("Cookie Num:" + index);
		return cookiePath + index;
	}

	// 写入文件
	public static void savePage(StringBuilder page, String outputpath, String fileName) {
		// 路径不存在则创建路径
		File filePath = new File(outputpath);
		if (!filePath.exists())
			filePath.mkdirs();

		File file2 = new File(outputpath + "/" + fileName);
		if (file2.exists())
			logger.warn("outfile exit!");
		else {
			FileOutputStream outputStream;
			try {
				outputStream = new FileOutputStream(file2);
				byte[] pageByte = null;
				try {
					pageByte = page.toString().getBytes();
					outputStream.write(pageByte);
				} catch (Exception e) {
					// TODO: handle exception
					logger.error(e);
				}
				outputStream.close();
			} catch (FileNotFoundException e) {
				logger.error(e);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

	// 加载配置文件
	private static void loadConfig() {
		Configuration.loadConfiguration(conPath);
		cookiePath = Configuration.getProperties("cookie_path");
		cookiesNum = Integer.parseInt(Configuration.getProperties("cookiesNum"));
		savePath = Configuration.getProperties("savePath");
		sleepTime = Integer.parseInt(Configuration.getProperties("sleepTime"));
		dbName = Configuration.getProperties("mysql_database");
		dbUser = Configuration.getProperties("mysql_user");
		dbPwd = Configuration.getProperties("mysql_password");
	}

	public static String GetMid(String url) {
		int index = url.indexOf("weibo.com") + 21;
		return url.substring(index, index + 9);
	}

	// 查询数据库
	public static String sqlList() {
		java.sql.Connection connection = DBConnection.getConnection(dbName, dbUser, dbPwd);
		String sql = null;
		sql = "select * from weibo_url where status=0";
		java.sql.PreparedStatement pstmt = DBConnection.getPreparedStatement(connection, sql);
		String url = null;
		int id = -1;
		try {
			java.sql.ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				id = rs.getInt(1);
				url = rs.getString(2);
			}
			rs.last();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (id >= 0) {
			sql = "update weibo_url set status=1 where id=" + id;
			pstmt = DBConnection.getPreparedStatement(connection, sql);
			try {
				pstmt.executeUpdate(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				DBConnection.close(connection, pstmt, null);
			}
		}
		
		try {
			pstmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return url;
	}
}
