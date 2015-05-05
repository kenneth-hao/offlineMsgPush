package com.yappam.openfire.plugin;

import java.io.File;

import org.jivesoftware.openfire.OfflineMessageListener;
import org.jivesoftware.openfire.OfflineMessageStrategy;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfflineMsgPushPlugin implements Plugin {
	
	private static final Logger log = LoggerFactory.getLogger(OfflineMsgPushPlugin.class);
	
	public static final String PUSH_KEY_URL = "plugin.offlineMsg.pushUrl";
	
	public static final String PUSH_KEY_PUSH_RESOURCES = "plugin.offlineMsg.pushResources";

	private OfflineMessageListener offlineMessageListener = null;

	@Override
	public void destroyPlugin() {
		if (offlineMessageListener != null) {
			OfflineMessageStrategy.removeListener(offlineMessageListener);
		}
	}

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		if (JiveGlobals.getProperty(PUSH_KEY_URL) == null) {
			JiveGlobals.setPropertyEncrypted(PUSH_KEY_URL, false);
			JiveGlobals.setProperty(PUSH_KEY_URL, "");
		}
		if (JiveGlobals.getProperty(PUSH_KEY_PUSH_RESOURCES) == null) {
			JiveGlobals.setPropertyEncrypted(PUSH_KEY_PUSH_RESOURCES, false);
			JiveGlobals.setProperty(PUSH_KEY_PUSH_RESOURCES, "");
		}
		
		offlineMessageListener = new OfflineMessageListenerImpl();
		OfflineMessageStrategy.addListener(offlineMessageListener);
	}

}