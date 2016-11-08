package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 读取配置文件的工具类
 * 提供了静态读取配置文件的方法
 * 改配置文件是放在每个worker节点
 * @author zeze
 *
 */
public class Configuration {
	private static Logger logger = Logger.getLogger(Configuration.class);
	private static Properties properties;
	private Configuration() {}

	/**
	 * 通过配置文件的路径获取相关的配置，存储在properties中，worker每次启动时调用该方法
	 * @param confPath 配置文件的路�?
	 */
	public static void loadConfiguration(String confPath) {
		//判断properties是否是空，如果不为空表示已经加载过配置文件，不需要再加载
		if(properties==null) {
			//利用InputStreamReader指定利用utf-8编码读取properties文件
			//这样是为了解决配置文件中的中文乱码问�?
			InputStream inputStream = null;
			InputStreamReader isr = null;
			try {
				inputStream = new FileInputStream(new File(confPath));
				isr = new InputStreamReader(inputStream, "UTF-8");
			} catch (FileNotFoundException e) {
				//文件未找到时，输出错误日志，并停止系
				logger.error(e);
				System.exit(0);
			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				System.exit(0);
			}
			properties = new Properties();
			try {
				//加载并存储到properties
				logger.info("load configuration from : "+ confPath);
				properties.load(isr);
				logger.info("load succeed!");
			} catch (IOException e) {
				logger.error(e);
			} finally {
				//关闭资源
				try {
					if(inputStream != null) {
						inputStream.close();
					}
					if(isr != null) {
						isr.close();
					}
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
	}
	
	/**
	 * 获取配置文件中某配置，若不存在返回null
	 * @param key 配置项的key
	 * @return
	 */
	public static String getProperties(String key) {
		String s = null;
		if(properties != null){
			s = properties.getProperty(key);
		}
		return s;
	}
}
