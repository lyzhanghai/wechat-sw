package com.desksoft.wechat.common.model.otherbean;

/**
 * 单月数据
 * 
 * @author Joker
 *
 */

public class MonthWaterInfo {
	
	// 水费年月
	private String yearMonth;
	// 上期抄见
	private String agoAmount;
	// 本期抄见
	private String currentAmount;
	// 抄表水量
	private String amount;
	//应收水量
	private String mustAmount;
	//金额
	private String fee;
	// 违约金
	private String wxFee;
	// 缴费标志
	private String flag;
	public String getYearMonth() {
		return yearMonth;
	}
	public void setYearMonth(String yearMonth) {
		this.yearMonth = yearMonth;
	}
	public String getAgoAmount() {
		return agoAmount;
	}
	public void setAgoAmount(String agoAmount) {
		this.agoAmount = agoAmount;
	}
	public String getCurrentAmount() {
		return currentAmount;
	}
	public void setCurrentAmount(String currentAmount) {
		this.currentAmount = currentAmount;
	}
	public String getAmount() {
		return amount;
	}
	public void setAmount(String amount) {
		this.amount = amount;
	}
	public String getFee() {
		return fee;
	}
	public void setFee(String fee) {
		this.fee = fee;
	}
	public String getWxFee() {
		return wxFee;
	}
	public void setWxFee(String wxFee) {
		this.wxFee = wxFee;
	}
	public String getFlag() {
		return flag;
	}
	public void setFlag(String flag) {
		this.flag = flag;
	}
	public String getMustAmount() {
		return mustAmount;
	}
	public void setMustAmount(String mustAmount) {
		this.mustAmount = mustAmount;
	}
}
