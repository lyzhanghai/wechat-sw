package com.desksoft.wechat.controller.microMsg;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.desksoft.wechat.common.model.Payrecord;
import com.desksoft.wechat.common.model.User;
import com.desksoft.wechat.service.XyBankPayService;
import com.desksoft.wechat.service.YouPuSocketService;
import com.jfinal.core.ActionKey;
import com.jfinal.core.Controller;
import com.jfinal.kit.HttpKit;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.weixin.sdk.kit.IpKit;
import com.jfinal.weixin.sdk.kit.PaymentKit;

/**
 * 兴业银行支付接口
 * @author Joker
 */
public class XyBankPayController extends Controller {
	private static Logger log = Logger.getLogger(XyBankPayController.class); 
	
/*	//商户相关资料
	private static String appid = "7551000001"; //商户号
	//商户密钥 
	private static String merchantKey = "9d101c97133837e13dde2d32a5054abb";*/
		
	private static String notify_url = "/pay/payNotify";
	
	/**
	 * 订单号 out_trade_no, openID, body,total_fee
	 */
	@ActionKey("/pay/xyPay")
	public void index() {
		
		// openId，采用 网页授权获取 access_token API：SnsAccessTokenApi获取
		String openId = getPara("openId");
		System.out.println(getPara("totalfee"));
		int num = mul(getPara("totalfee"),"100");
		// 统一下单文档地址：https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_1
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("service", "pay.weixin.jspay");
		params.put("version", "1.0");
		params.put("charset", "UTF-8");
		params.put("sign_type", "MD5");
		params.put("mch_id", PropKit.get("xy_appid","7551000001"));
		params.put("is_raw", "0");
		//商户订单号 必填
		String userNo = getPara("userNo");
		//String userNo="222";
		long currentTime = System.currentTimeMillis() ;
		String flag = getPara("flag");
		String out_trade_no = flag+XyBankPayService.formatTimeStamp(new Date(currentTime))+"-"+userNo;
		//out_trade_no+=;
	//	System.out.println(out_trade_no);
		params.put("out_trade_no",out_trade_no);
		//params.put("out_trade_no","A20160328135618-904926");
		
		
		//终端设备号，非必填
		params.put("device_info", "");
		
		//测试时该参数不需要
		
		String devType = PropKit.get("devType");
		if("online".equalsIgnoreCase(devType)){
			//附加信息
			params.put("attach", "水务微信支付"); 
			//用户openId
			params.put("sub_openid", openId);
			//总费用，分为单位 //getPara("total_fee")
			params.put("total_fee", String.valueOf(num));
		}else{
			//附加信息
			params.put("attach", "test"); 
			params.put("total_fee", "1");
		}
		
		
		int type = 2;
		if("A".equals(flag)){
			type =1;
			// 商品描述 必填
			params.put("body", "水务缴费");
		}else{
			// 商品描述 必填
			params.put("body", "水务预存");
		}
		
		//欠费笔数
		 int realNumber = getParaToInt("realNumber",0);
		 //费用IDs
		 String feeIDs = getPara("feeIDs");
		//插入数据库
		if(Payrecord.dao.addPayRecord(type, openId, userNo, num, 0, out_trade_no,realNumber,feeIDs)){
			log.info("支付前插入数据库成功"+out_trade_no);
		}else{
			log.info("支付前插入数据库失败"+out_trade_no);
		}
		
		String ip = IpKit.getRealIp(getRequest());
		if (StrKit.isBlank(ip)) {
			ip = "127.0.0.1";
		}
		params.put("mch_create_ip", ip);
		
		System.out.println("===========notify_url:"+XyBankPayService.getNoticeUrl()+ notify_url);
		params.put("notify_url", XyBankPayService.getNoticeUrl()+ notify_url);
		params.put("callback_url", XyBankPayService.getNoticeUrl()+"/pages/user/paysuccess.jsp");
		params.put("time_start", XyBankPayService.formatTimeStamp(new Date(currentTime)));
		long currentTime2 = currentTime + 30*60*1000;
		params.put("time_expire", XyBankPayService.formatTimeStamp(new Date(currentTime2)));
		
		//商品标记
		params.put("goods_tag", "");
		//随机字符串
		params.put("nonce_str", "desksoft");
		
		String sign = PaymentKit.createSign(params, PropKit.get("xy_merchantKey","9d101c97133837e13dde2d32a5054abb"));
		params.put("sign", sign);
		
		String xmlResult = XyBankPayService.pushOrder(params);
		
		System.out.println(xmlResult);
		Map<String, String> result = PaymentKit.xmlToMap(xmlResult);
		
		String status = result.get("status");
		String message = result.get("message");
		if (!("0".equals(status)||"0"==status)) {
			if(StrKit.isBlank(message)){
				renderText("连接失败");
			}else{
				renderText("签名失败参数格式校验错误");
			}
			
			return;
		}
		String result_code = result.get("result_code");
		String err_code = result.get("err_code");
		String err_msg = result.get("err_msg");
		if (!("0".equals(result_code)||"0"==result_code)) {
			renderText("错误代码："+err_code+",错误说明："+err_msg);
			return;
		}
		// 以下字段在status 和result_code都为0的时候有返回
		String token_id = result.get("token_id");
		if(StrKit.isBlank(token_id)){
			renderText("连接失败");
			return;
		}
		String chkPayUri = XyBankPayService.getPayUri(token_id);
		
		redirect(chkPayUri);
	}
	
	@ActionKey("/pay/payNotify")
	public void pay_notify() {
		// 支付结果通用通知文档: https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=9_7
		String xmlMsg = HttpKit.readData(getRequest());
		System.out.println("支付通知="+xmlMsg);
		Map<String, String> result = PaymentKit.xmlToMap(xmlMsg);
		String status = result.get("status");
		String message = result.get("message");
		if (!("0".equals(status)||"0"==status)) {
			if(StrKit.isBlank(message)){
				renderText("连接失败");
			}else{
				renderText("签名失败参数格式校验错误");
			}
			
			renderText("fail");
		}
		String result_code = result.get("result_code");
		String err_code = result.get("err_code");
		String err_msg = result.get("err_msg");
		if (!("0".equals(result_code)||"0"==result_code)) {
			renderText("错误代码："+err_code+",错误说明："+err_msg);
			renderText("fail");
		}
		// 以下字段在status 和result_code都为0的时候有返回
		String out_trade_no = result.get("out_trade_no");
		Payrecord pay =  Payrecord.dao.getPayRecordByRemark(out_trade_no);
		if(pay!=null){
			//	isPaySuccess` tinyint(4) DEFAULT '0' COMMENT '进入支付接口时插入数据，0未支付，返回消息时，再次update为1，已支付',
			int isPaySuccess = pay.getIsPaySuccess();//防止重复发送
			if(0==isPaySuccess){
				//返回信息成功，更新数据库isPaySuccess，
				String out_transaction_id = result.get("out_transaction_id");
				
				updateLocalDataOfOrder(pay,out_transaction_id);
				//调用优普接口，通知水厂
				dealMessageForYpInterface(pay);
			}
		}
		
		renderText("success");
	}
	
	
	private void updateLocalDataOfOrder(Payrecord pay,String out_transaction_id){
		pay.set("isPaySuccess", 1).set("orderpayNo", out_transaction_id).update();
		//调用水务的支付成功接口;还要加入积分制1元一分
		if(pay.getType()==2){//预存 分转元
			int num = 0;
			if(pay.getFee()!=null&&pay.getFee()!=0){
				num = (int)pay.getFee()/100;
			}
			User.dao.addScoreByOpenId(pay.getOpenId(),num);
		}
	} 
	
	
	private void dealMessageForYpInterface(Payrecord pay){
		//支付日期
		Date time = pay.getUpdateTime()==null?new Date():pay.getUpdateTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String payDate = sdf.format(time);
		
		//实际付费
		float num= (float)pay.getFee()/100;   
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数   
		String fee = df.format(num);//返回的是String类型 
		
		if(pay.getType()==2){//预存
			YouPuSocketService.getRePayData(pay.getUserNo().toString(), pay.getRemark(), fee, payDate);
		}else{
			//缴费确认 
			YouPuSocketService.getchkpay(pay.getRemark(), pay.getRealNumber().toString(), fee,  payDate,pay.getFeeIDs());
		}
	}
	
	 private static int mul(String v1, String v2) {  
	        BigDecimal b1 = new BigDecimal(v1);  
	        BigDecimal b2 = new BigDecimal(v2);  
	        return b1.multiply(b2).intValue();  
	 }
}
