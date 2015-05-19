package com.yappam.openfire.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
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
	
	private static final Logger logger = LoggerFactory.getLogger(OfflineMessageListenerImpl.class);

	@Override
	public void messageBounced(Message message) {
		// Nothing
	}

	@Override
	public void messageStored(Message message) {
		// 系统配置的可推送Resource 来源
		String pushResourcesStr = JiveGlobals.getProperty(OfflineMsgPushPlugin.PUSH_KEY_PUSH_RESOURCES);
		if (pushResourcesStr == null || StringUtils.isEmpty(pushResourcesStr)) {
			logger.error("推送消息的 Resource 来源未设置, 消息推送失败.");
			return ;
		}
		String[] pushResources = pushResourcesStr.split("\\s*,\\s*");
		if (pushResources.length == 0) {
			logger.error("推送消息的 Resource 格式设置错误, 多个Resource, 请用','分割.");
			return ;
		}
		
		// 推送目标的Resource
		String targetResource = message.getFrom().getResource();
		// 如果不是需要推送的资源, 则不推送任何消息
		if (ArrayUtils.contains(pushResources, targetResource) == false) {
			logger.error("推送消息的 Resource 不支持[{}]. Resource来源仅支持 {}", targetResource, Arrays.toString(pushResources));
			return ;
		}
		
		// 发送人 JID
		JID fromJid = message.getFrom();
		
		// 接收人 JID
		JID toJid = message.getTo();
		
		String pushUrl = JiveGlobals.getProperty(OfflineMsgPushPlugin.PUSH_KEY_URL);
		if (StringUtils.isEmpty(pushUrl)) {
			logger.error("请初始化消息推送的服务器地址! 请设置系统属性<{}>", OfflineMsgPushPlugin.PUSH_KEY_URL);
			return ;
		}
		
		try {
		    HttpParams httpParameters = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000); //设置请求超时10秒
		    HttpConnectionParams.setSoTimeout(httpParameters, 10*1000); //设置等待数据超时10秒
		    HttpConnectionParams.setSocketBufferSize(httpParameters, 8192);
		    HttpClient httpClient = new DefaultHttpClient(httpParameters); //此时构造DefaultHttpClient时将参数传入 
		    
			HttpPost post = new HttpPost(pushUrl);
			logger.info("离线消息推送地址: {}", pushUrl);
			
			post.getParams().setParameter("http.protocol.content-charset",HTTP.UTF_8);  
			post.getParams().setParameter(HTTP.CONTENT_ENCODING, HTTP.UTF_8);  
			post.getParams().setParameter(HTTP.CHARSET_PARAM, HTTP.UTF_8);  

			// 参数内容
			String sendMsg = String.format("{\"from\":\"%s\", \"to\":\"%s\", \"message\":\"%s\"}", fromJid.getNode(), toJid.getNode(), message.getBody());
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("msg", sendMsg));
			
			logger.info("离线消息推送内容: {}", sendMsg);
	
			post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
			
			HttpResponse response = httpClient.execute(post);
			
			int status = response.getStatusLine().getStatusCode();
			if (status == 200) {
				logger.info("离线消息推送成功.");
			} else {
				logger.error("离线消息推送失败, HttpStatusCode: {}", status);
			}
			
			httpClient.getConnectionManager().shutdown(); 
		} catch (IllegalStateException e) {
			String errMsg = String.format("请确认消息推送服务器地址 [%s] 是否可用 \n StackOverflow: %s ", pushUrl, e.getMessage());
			logger.error(errMsg);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
