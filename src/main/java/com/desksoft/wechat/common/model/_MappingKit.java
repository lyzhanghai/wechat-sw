package com.desksoft.wechat.common.model;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;

/**
 * Generated by JFinal, do not modify this file.
 * <pre>
 * Example:
 * public void configPlugin(Plugins me) {
 *     ActiveRecordPlugin arp = new ActiveRecordPlugin(...);
 *     _MappingKit.mapping(arp);
 *     me.add(arp);
 * }
 * </pre>
 */
public class _MappingKit {

	public static void mapping(ActiveRecordPlugin arp) {
		arp.addMapping("payrecord", "id", Payrecord.class);
		arp.addMapping("refeeflow", "id", Refeeflow.class);
		arp.addMapping("scoreflow", "id", Scoreflow.class);
		arp.addMapping("shutoffwaterflow", "id", Shutoffwaterflow.class);
		arp.addMapping("user", "id", User.class);
		arp.addMapping("userbindflow", "id", Userbindflow.class);
	}
}

