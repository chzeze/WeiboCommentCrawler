package weiboextractor;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 
 * @ClassName: WeiboExtrator
 * @Description: 微博抽取
 * @author zeze
 * @date 2016年11月27日 上午12:09:49
 *
 */

public class WeiboExtrator {

	private static Logger logger = Logger.getLogger(WeiboExtrator.class);

	// 热评翻页抽取
	public int weiboExtratorPageNum(StringBuilder html) {
		int num = 1;
		Document doc = Jsoup.parse(html.toString());

		// 没有热门评论,直接返回
		Elements rt = doc.select("div[class=c]");
		if (!rt.text().contains("返回评论列表")) {
			logger.info("没有转发");
			return 0;
		}

		if (doc.select("[id=pagelist]").text().contains("页")) {// 存在翻页
			String pnum = doc.select("[id=pagelist]").get(0).text();
			pnum = pnum.substring(pnum.indexOf("/") + 1).replace("页", "");
			num = Integer.parseInt(pnum);

		} else {
			logger.info("热门评论小于10");
		}
		logger.info("热门评论页数：" + num);
		return num;
	}

	// 用户名
	public String weiboExtratorUserName(StringBuilder html) {
		String  name = null;
		Document doc = Jsoup.parse(html.toString());
		if (doc.select("div[class=ut]").text().length()>0) {
			name=doc.select("div[class=ut]").text();
		} else {
			logger.info("没有用户名");
		}
		logger.info("当前用户：" + name);
		return name;
	}

	// 评论列表的抽取
	public Set<String> weiboExtratorCommentList(StringBuilder html) {
		Set<String> commentList = new HashSet<String>();
		String zname = null;// 评论的用户名
		String zid = null;// 评论的用户ID
		String zzan = null;// 点赞数
		String zmid = null;// 评论的消息id
		String ztime = null;// 评论时间
		String zsource = null;// 来源
		String ztext = null;// 转发的内容
		int cnt = 0;
		Document doc = Jsoup.parse(html.toString());
		Elements RTList = doc.select("div[class =c]");
		for (Element result : RTList) {// 解析列表
			// 点赞数
			zzan = result.select("span[class=cc]").text();
			if (zzan.equals("")) {// 过滤没有点赞标签
				continue;
			}
			zzan = zzan.trim().substring(1).replace("[", "").replace("]", "").replace(" 回复", "");
			if (result.select("a").size() > 0) {
				zname = result.select("a").get(0).text();// 转发的用户名
				zid = result.select("a").get(0).toString();// 转发的用户id
				if (zid.indexOf("u/") == 10) {// 正常的用户id
					zid = zid.substring(zid.indexOf("\">") - 10, zid.indexOf("\">"));
				} else {
					zid = zid.substring(zid.indexOf("/") + 1, zid.indexOf("\">"));
				}
			}
			if (result.text().contains("查看更多热门"))
				continue;
			// 转发时间和来源
			String tmp = result.select("span[class=ct]").text();
			ztime = tmp.substring(0, tmp.indexOf("来自")).replace(" ", "");
			zsource = tmp.substring(tmp.indexOf("来自") + 2);
			// 转发的消息id
			zmid = result.select("span[class=cc]").toString();
			zmid = zmid.substring(zmid.indexOf("attitude") + 9, zmid.indexOf("attitude") + 18);
			// 转发的内容
			ztext = result.text();
			if (ztext.contains("//@")) {
				ztext = ztext.substring(ztext.indexOf(":") + 1, ztext.indexOf("//@")).replace(" ", "");
			} else {
				ztext = ztext.substring(ztext.indexOf(":") + 1, ztext.indexOf("   举报   ")).replace(" ", "");
			}
			// 用户ID,用户名,消息ID,消息内容,来源,赞数,发布时间
			// zid,zname,zmid,ztext,zsource,zzan,ztime
			cnt++;
			String wString = zid + "|" + zname + "|" + zmid + "|" + ztext + "|" + zsource + "|" + zzan + "|" + ztime;
			commentList.add(wString);
//			logger.info(cnt + ":" + wString);
		}
		return commentList;
	}

}
