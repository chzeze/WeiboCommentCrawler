package weibosumit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.http.client.CookieStore;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.gargoylesoftware.htmlunit.util.Cookie;

import util.Configuration;
import util.DBConnection;
import weiboextractor.WeiboExtrator;

/***
 * 
 * @ClassName: CrawlWeiboComment
 * @Description: 微博用户操作发微博
 * @author zeze
 * @date 2016年11月27日11:55:43
 *
 */
public class WeiboOption {

	private static Logger logger = Logger.getLogger(WeiboOption.class);
	private static String conPath = "/workspace/AliExpressCrawl/configuration.properties";
	private static String cookiePath;// cookie目录
	private static int auto_userId;// 自动机的用户
	private static String zanNum;// 发布点赞数超过一定限额的
	private static String userSt;// 自动机用户凭证
	private static int sleepTime;// 文件保存目录
	private static String dbName;// 数据库名
	private static String dbUser;// 数据库用户名
	private static String dbPwd;// 数据库密码

	private static SimpleDateFormat dayform = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) {

		if (args.length == 1) {
			conPath = args[0];
		}
		logger.info("Config Path:" + conPath);

		while (true) {
			// 加载配置文件
			loadConfig();
			String cookie = GetCookieStore();
			StringBuilder html = GetMothod("http://weibo.cn/pub", cookie);
			WeiboExtrator extrator = new WeiboExtrator();
			String userName = extrator.weiboExtratorUserName(html);
			System.out.println(userName);
			System.out.println("当前用户cookie值：" + cookie);
			String text = null;
			int cnt = 0;

			List<String> commentList = new ArrayList<String>();
			commentList = queryHotComment();// 查询数据库
			Iterator<String> it = commentList.iterator();
			while (it.hasNext()) {
				text = it.next();
				if (text.contains("http") || text.contains("回复"))
					continue;
				System.out.println(text);
				cnt++;
				PostWeibo(cnt, userSt, text, cookie);//发布微博
				sleepTime(sleepTime);
			}
		}
	}

	// 采集睡眠时间
	private static void sleepTime(int i) {
		// TODO Auto-generated method stub
		try {
			Random random = new Random();
			i = i + random.nextInt(10 * i);
			logger.info("Sleep Time:" + i);
			Thread.sleep(i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int PostWeibo(int cnt, String userSt, String text, String cookie) {
		String url = "http://weibo.cn/mblog/sendmblog?st=" + userSt;
		String rl = "0";
		String content = text;
		HttpClient httpClient = new HttpClient();
		try {
			PostMethod postMethod = getPostMethodHeader(url, cookie);
			postMethod.setParameter("rl", rl);
			postMethod.setParameter("content", content);

			int statusCode = httpClient.executeMethod(postMethod);// 返回状态码200为成功，500为服务器端发生运行错误
			System.out.println(cnt + ":返回状态码：" + statusCode);

			// 处理返回的页面
			InputStream inputStream = postMethod.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			StringBuffer stringBuffer = new StringBuffer();
			String str = "";
			while ((str = br.readLine()) != null) {
				stringBuffer.append(str);
			}
			Document doc = Jsoup.parse(stringBuffer.toString());
			if (statusCode == 200) {
				System.out.println(doc.text());
			}
			if (doc.text().contains(dayform.format(new Date()) + " 微博 操作失败 如果没有自动跳转")) {
				System.out.println(doc.text());
				return 0;
			}

			String redirectLocation;
			Header locationHeader = postMethod.getResponseHeader("Location");
			if (locationHeader != null) {
				redirectLocation = locationHeader.getValue();
				if (redirectLocation.indexOf("http://m.weibo.cn/security") != -1) {
					System.out.println(dayform.format(new Date()) + " 账号被封!");
					return 0;
				}
				System.out.println(dayform.format(new Date()) + " 发布微博成功,重定向url:" + redirectLocation);
				logger.info(" 发布微博成功,重定向url:" + redirectLocation);
				// GetMothod(redirectLocation, cookie);
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return 1;
	}

	private static PostMethod getPostMethodHeader(String url, String cookie) {
		PostMethod postMethod = new PostMethod(url);
		try {
			postMethod.getParams().setContentCharset("utf-8");
			// 每次访问需授权的网址时需 cookie 作为通行证
			postMethod.setRequestHeader("cookie", cookie);
			postMethod.setRequestHeader("Accept-Language", "zh-CN,zh;q=0.8");
			postMethod.setRequestHeader("Connection", "keep-alive");
			postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			postMethod.setRequestHeader("Host", "weibo.cn");
			postMethod.setRequestHeader("Origin", "http://weibo.cn");
			postMethod.setRequestHeader("Upgrade-Insecure-Requests", "1");
			postMethod.setRequestHeader("Referer", "http://weibo.cn/");
			postMethod.setRequestHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0");

			postMethod.setFollowRedirects(false);
		} catch (Exception e) {
			logger.error(e);
		}
		return postMethod;
	}

	public static StringBuilder GetMothod(String url, String cookie) {
		HttpClient httpClient = new HttpClient();
		StringBuilder stringBuffer = new StringBuilder();
		try {
			GetMethod getMethod = new GetMethod(url);
			getMethod.getParams().setContentCharset("utf-8");
			getMethod.setRequestHeader("cookie", cookie);
			getMethod.setRequestHeader("Accept-Language", "zh-cn");
			getMethod.setRequestHeader("Host", "weibo.cn");
			getMethod.setRequestHeader("Origin", "http://weibo.cn");
			getMethod.setRequestHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0");
			int statusCode = httpClient.executeMethod(getMethod);// 返回状态码200为成功，500为服务器端发生运行错误
			System.out.println("返回状态码：" + statusCode);
			// 打印出返回数据，检验一下是否成功

			// 处理返回的页面
			InputStream inputStream = getMethod.getResponseBodyAsStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

			String str = "";
			while ((str = br.readLine()) != null) {
				stringBuffer.append(str);
			}
			if (statusCode == 200) {
				Document doc = Jsoup.parse(stringBuffer.toString());
				// System.out.println(dayform.format(new Date()) + "发布微博成功");
				// System.out.println(doc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stringBuffer;
	}

	// 加载配置文件
	private static void loadConfig() {
		Configuration.loadConfiguration(conPath);
		cookiePath = Configuration.getProperties("cookie_path");
		auto_userId = Integer.parseInt(Configuration.getProperties("auto_userId"));
		userSt = Configuration.getProperties("auto_userSt");
		sleepTime = Integer.parseInt(Configuration.getProperties("auto_sleepTime"));
		dbName = Configuration.getProperties("mysql_database");
		dbUser = Configuration.getProperties("mysql_user");
		dbPwd = Configuration.getProperties("mysql_password");
		zanNum = Configuration.getProperties("auto_zan");
	}

	// 随机获取cookies
	private static String GetCookieStore() {
		String cookieStr = "";
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
			List<org.apache.http.cookie.Cookie> l = cookieStore.getCookies();
			String tempstr = "";
			for (org.apache.http.cookie.Cookie temp1 : l) {
				Cookie cookie = new Cookie(temp1.getDomain(), temp1.getName(), temp1.getValue(), temp1.getPath(),
						temp1.getExpiryDate(), false);
				tempstr = cookie.toString().substring(0, cookie.toString().indexOf("domain"));
				cookieStr += tempstr;
				// System.out.println(cookie);
			}
		} else {
			logger.error("CookiePath doesn`t exit !!!");
		}
		cookieStr = cookieStr.substring(0, cookieStr.length() - 1);
		return cookieStr;
	}

	// 随机获取cookie值
	private static String cookiePathAppendRandom() {
		Random random = new Random();
		logger.info("Cookie Num:" + auto_userId);
		System.out.println("Cookie Index:" + auto_userId);
		return cookiePath + auto_userId;
	}

	public static List<String> queryHotComment() {
		java.sql.Connection connection = DBConnection.getConnection(dbName, dbUser, dbPwd);
		String sql = "select text,zan,createtime from weibo_comment where zan>" + zanNum
				+ " order by createtime desc limit 0,1000";
		java.sql.PreparedStatement pstmt = DBConnection.getPreparedStatement(connection, sql);

		System.out.println(sql);
		List<String> pornlist = new ArrayList<String>();
		String text = null;
		try {
			Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			java.sql.ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				text = rs.getString(1);
				pornlist.add(text);
			}
			rs.last();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBConnection.close(connection, pstmt, null);
		}
		return pornlist;
	}

}
