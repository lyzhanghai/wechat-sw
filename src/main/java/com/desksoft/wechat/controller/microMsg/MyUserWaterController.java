package com.desksoft.wechat.controller.microMsg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.desksoft.wechat.common.model.Userbindflow;
import com.desksoft.wechat.common.model.otherbean.MonthWaterInfo;
import com.desksoft.wechat.common.model.otherbean.MyWaterInfo;
import com.desksoft.wechat.common.utils.ArithUtils;
import com.desksoft.wechat.controller.admin.BaseController;
import com.desksoft.wechat.service.YouPuSocketService;
import com.jfinal.kit.StrKit;


/**
 * 我的用水 缴费 预存
 * @author Joker
 *
 */
public class MyUserWaterController extends BaseController {
	
	/**
	 * 加载我的用水列表
	 */
	public void getUsedWaterList(){
		String openId = getPara("openId",null);
		if(StringUtils.isNotBlank(openId)){
			//1.rlist 接收绑定的1个或者多个水表号的信息即
			List<MyWaterInfo> rlist = new ArrayList<MyWaterInfo>();
			List<Userbindflow> list = Userbindflow.me.selectBindingUserList(openId);
			for(Userbindflow userBindFlow:list){
				//2.1户号 
				String userNo = userBindFlow.getUserNo().toString().trim();
				 //调用 水务接口，根据openID，找到绑定的所有户号，每个户号去调用接口
				if(userBindFlow!=null&&StrKit.notBlank(userNo)){
					String backString = YouPuSocketService.getUserWaterInfo(userNo,null,null);
					//响应码
					String code =YouPuSocketService.isSuccessConnect(backString);
					if("00".equals(code)){
						//返回数据拼接为List
						List<?> dataList = YouPuSocketService.getBackList(backString);
						//2.2预存款
						double rePay = Double.parseDouble((String)dataList.get(2));
						//2.3卡号
						String cardID = (String)dataList.get(4);
						
						// 一共有几个绑定表号
						int totalTable =  Integer.valueOf((String)dataList.get(3));
						int d1=1;
						for(int i=4; i<dataList.size();i++){
							if(d1>totalTable){
								break;
							}
							//2.myWaterInfo接收单个水表信息，是1个水表的最近12月信息()
							MyWaterInfo myWaterInfo = new MyWaterInfo();
							if(StrKit.isBlank(userNo)){
								userNo = (String)dataList.get(1);
							}
							myWaterInfo.setUserNo(userNo);
							myWaterInfo.setCardNo(cardID);
							myWaterInfo.setRePay(rePay);
							//2.4 户名
							String userName = dataList.get(i+1).toString();
							myWaterInfo.setUserName(userName);
							//2.5 地址
							String address = dataList.get(i+2).toString();
							myWaterInfo.setAddress(address);
							//2.6 年月水表数据流
							List<MonthWaterInfo> dList = new ArrayList<MonthWaterInfo>();
							myWaterInfo.setWaterList(dList);
							int totalData =  Integer.valueOf((String)dataList.get(i+3));
							int d2=1;
							for(int j=8;j<dataList.size();j+=8){
								if(d2>totalData){
									break;
								}
								MonthWaterInfo monthWaterInfo = new MonthWaterInfo();
								monthWaterInfo.setYearMonth(dataList.get(j).toString());
								monthWaterInfo.setAgoAmount(dataList.get(j+1).toString());
								monthWaterInfo.setCurrentAmount(dataList.get(j+2).toString());
								monthWaterInfo.setAmount(dataList.get(j+3).toString());
								monthWaterInfo.setMustAmount(dataList.get(j+4).toString());
								monthWaterInfo.setFee(dataList.get(j+5).toString());
								monthWaterInfo.setWxFee(dataList.get(j+6).toString());
								String flag = dataList.get(j+7).toString().trim();
								if(flag.contains("1")){
									flag="1";
								}else if(flag.contains("0")){
									flag="0";
								}
								monthWaterInfo.setFlag(flag);
								dList.add(monthWaterInfo);
								d2++;
							}
							i+=totalData*8+8;
							rlist.add(myWaterInfo);
							d1++;
						}
						
					}else{
						renderText("系统获取数据失败");
					}
				}else{
					renderText("你绑定的户号已经无效");
				}
			
			}
			
			if(list.size()>0){
				setAttr("result", rlist);
				render("myUsedWater.html");
			}else{
				render("addUser.html");
			}
				
		}else{
			renderText("非法URL");
		}
		
	}
	
	/**
	 * 缴费 openId查询该用户对应的户号List，依次预存查看
	 */
	public void payWater(){
		String openId = getPara("openId",null);
		if(StringUtils.isNotBlank(openId)){
			//setAttr("openID", openId);
			List<Map<String,Object>> rlist = new ArrayList<Map<String,Object>>();
			List<Userbindflow> list = Userbindflow.me.selectBindingUserList(openId);
			for(Userbindflow userBindFlow:list){
				 // 调用 水务接口，根据openID，找到绑定的所有户号，每个户号去调用接口,返回每个用户的欠费情况
				//假设调用接口，返回用水信息wtMap和一个状态值status:1不欠费，0欠费，欠费就有一个wtMap
				String userNo = userBindFlow.getUserNo().toString().trim();
				Map<String,Object> map = new HashMap<String,Object>();
				
				if(userBindFlow!=null&&StrKit.notBlank(userNo)){
					map.put("userBindFlow", userBindFlow);
					
					String backString = YouPuSocketService.getpayData(userNo, null, null);
					//响应码
					String code =YouPuSocketService.isSuccessConnect(backString);
					List<?> dataList = YouPuSocketService.getBackList(backString);
					if("00".equals(code)){
						//绑定人信息
						if(StrKit.isBlank(userBindFlow.getUserName())){
							userBindFlow.setUserName(dataList.get(1).toString());
						}
						if(StrKit.isBlank(userBindFlow.getAddress())){
							userBindFlow.setAddress(dataList.get(2).toString());
						}
						//其他信息
						String num = (String) dataList.get(3);//一共多少笔交易
						double    waterFee = 0;
						double    fund = 0;
						double    total;
						String feeIDs ="";
						for(int i=4;i<dataList.size();i+=4){
							//费用ID 
							feeIDs+=("|"+(String) dataList.get(i));
							waterFee = ArithUtils.add(waterFee,Double.parseDouble((String) dataList.get(i+2)));
							fund = ArithUtils.add(fund,Double.parseDouble((String) dataList.get(i+3)));
						}
						total = ArithUtils.add(waterFee,fund);
						Map<String,Object> wtMap = new HashMap<String,Object>();
						wtMap.put("num", num);//笔数
						wtMap.put("waterFee", waterFee);//水费
						wtMap.put("fund", fund);//违约金
						wtMap.put("total", total);//总金额
						wtMap.put("feeIDs", feeIDs);//全部费用IDs
						map.put("wtMap", wtMap);
						if(Integer.valueOf(num)>0){
							map.put("status", 0);
						}else{
							map.put("status", 1);
						}
						
					}else if("11".equals(code)){
						//绑定人信息
						if(StrKit.isBlank(userBindFlow.getUserName())){
							userBindFlow.setUserName(dataList.get(1).toString());
						}
						if(StrKit.isBlank(userBindFlow.getAddress())){
							userBindFlow.setAddress(dataList.get(2).toString());
						}
						map.put("status", 1);
					}else{
						map.put("status", -1);//查询失败
					}
				}	
				//该用户用水信息
				rlist.add(map);
			}
			
			if(list.size()>0){
				setAttr("objList", rlist);
				render("payWater.html");
			}else{
				render("addUser.html");
			}
				
		}else{
			renderText("非法URL");
		}
		
	}
	
	/**
	 * 水费预存 openId查询该用户对应的户号List，依次预存查看
	 */
	public void rePayWater() {
		String openId = getPara("openId", null);
		if (StringUtils.isNotBlank(openId)) {
			List<Userbindflow> list = Userbindflow.me.selectBindingUserList(openId);

			for (Userbindflow userBindFlow : list) {
				// 调用 水务接口，根据openID，找到绑定的所有户号，每个户号去调用接口,返回每个用户的预存信息
				String userno = userBindFlow.getUserNo().toString();
				if (StrKit.notBlank(userno)) {
					String temp = YouPuSocketService.getUserInfoData(userno);
					String[] temps = temp.split("\\|");
					// 响应码
					String ReturnCode = temps[0].substring(42, 44);
					if ("00".equals(ReturnCode)) {
						// 把预存金额，存入本地预存费用
						String rePayFee = temps[5].toString();
						userBindFlow.setRePayFee(new BigDecimal(rePayFee));
						// 该用户用水信息预存款
						userBindFlow.set("updateTime", new Date()).update();
						// 调用接口，返回用水信息预存款 失败给remark赋值
						userBindFlow.setRemark("1");
					} else {
						// 调用接口，返回用水信息预存款 失败给remark赋值
						userBindFlow.setRemark("0");
					}

				}
			}

			if (list.size() > 0) {
				setAttr("objList", list);
				render("rePayWater.html");
			} else {
				render("addUser.html");
			}

		} else {
			renderText("非法URL");
		}
	}
	
}
