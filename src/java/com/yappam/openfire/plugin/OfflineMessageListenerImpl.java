package com.yappam.openfire.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.jivesoftware.openfire.OfflineMessageListener;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public class OfflineMessageListenerImpl implements OfflineMessageListener {
	
	private static final Logger log = LoggerFactory.getLogger(OfflineMessageListenerImpl.class);

	@Override
	public void messageBounced(Message message) {
		// Nothing
	}

	@Override
	public void messageStored(Message message) {
		String pushResource = JiveGlobals.getProperty(OfflineMsgPushPlugin.PUSH_KEY_PUSH_RESOURCES);
		String  targetResource = message.getFrom().getResource();
		// 如果不是需要推送的资源, 则不推送任何消息
		if (pushResource == null || pushResource.equals(targetResource) == false) {
			return ;
		}
		
		JID fromJid = message.getFrom();
		JID toJid = message.getTo();
		String sendMsg = String.format("{\"from\":\"%s\", \"to\":\"%s\", \"message\":\"%s\"}", fromJid.getNode(), toJid.getNode(), message.getBody());
		
		System.out.println("PARAM >> msg : " + sendMsg);
		log.info("PARAM >> msg : " + sendMsg);
		
		String pushUrl = JiveGlobals.getProperty(OfflineMsgPushPlugin.PUSH_KEY_URL);
		
		try {
		    HttpParams httpParameters = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000); //设置请求超时10秒
		    HttpConnectionParams.setSoTimeout(httpParameters, 10*1000); //设置等待数据超时10秒
		    HttpConnectionParams.setSocketBufferSize(httpParameters, 8192);
		    HttpClient httpClient = new DefaultHttpClient(httpParameters); //此时构造DefaultHttpClient时将参数传入 
			
		    
		    if (StringUtils.isEmpty(pushUrl)) {
				System.out.println("请初始化消息推送的服务器地址!");
				log.error("请初始化消息推送的服务器地址!");
			}
		    
			HttpPost post = new HttpPost(pushUrl);
			
			post.getParams().setParameter("http.protocol.content-charset",HTTP.UTF_8);  
			post.getParams().setParameter(HTTP.CONTENT_ENCODING, HTTP.UTF_8);  
			post.getParams().setParameter(HTTP.CHARSET_PARAM, HTTP.UTF_8);  

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("msg", sendMsg));
	
			post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
			
			HttpResponse response = httpClient.execute(post);
			
			System.out.println(response.getStatusLine());
			log.info("RESPONSE STATUS >> " + response.getStatusLine());
			
			httpClient.getConnectionManager().shutdown(); 
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
			String errMsg = String.format("请确认消息推送服务器地址 [%s] 是否可用 \n %s ", pushUrl, e.getMessage());
			System.out.println(errMsg);
			log.error(errMsg);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			log.error(e.getMessage());
		}
	}

}
