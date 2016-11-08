package client;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class AliExpressExtract {
	private Logger logger = Logger.getLogger(AliExpressExtract.class);
	
	public int extractPage(StringBuilder html){
		int max=1;
		Document doc=Jsoup.parse(html.toString());
		Elements Rlist = doc.select("div[class=ui-pagination-navi util-left]");
		if(Rlist.size()>0){
			Rlist=Rlist.select("a");
			for (Element result : Rlist) {
				if(result.text().equals("Next"))
					continue;
				try {
				    int a = Integer.parseInt(result.text().toString());
				    max=a>max?a:max;
				} catch (NumberFormatException e) {
					logger.error(e);
				}
				logger.info("extractPage:https:"+result.attr("href"));
			}
		}else{
			logger.info("Page Size:"+Rlist.size());
		}
		return max;
	}
	
	public void extractProduct(StringBuilder html){
		int max=1;
		Document doc=Jsoup.parse(html.toString());
		Elements Rlist = doc.select("li[class=item]");
		System.out.println("Size:"+Rlist.size());
		if(Rlist.size()>0){
			for (Element result : Rlist) {
			
			}
		}
	}

	public boolean loginCheck(StringBuilder builder) {
		// TODO Auto-generated method stub
		Document doc = Jsoup.parse(builder.toString());
        if(doc.select("title").text().startsWith("Buy Products")){
        	return false;
        }else{
        	return true;
        }
		
	}

}
