package com.desksoft.wechat.controller.admin;

import com.jfinal.core.ActionKey;


//@Before(IocInterceptor.class)
public class IndexController extends BaseController {



	public void index() {
		setAttr("onlineUser", getOnLineUser());
		render("index.html");
	}
	@ActionKey("/index")
	public void index2() {
		setAttr("onlineUser", getOnLineUser());
		render("index.html");
	}
	
}