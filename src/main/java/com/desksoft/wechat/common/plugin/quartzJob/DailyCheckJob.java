package com.desksoft.wechat.common.plugin.quartzJob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.desksoft.wechat.common.model.Payrecord;
import com.desksoft.wechat.common.plugin.Scheduled;
import com.desksoft.wechat.common.utils.FileOperation;
import com.desksoft.wechat.common.utils.FtpBusiness;
import com.desksoft.wechat.service.YouPuSocketService;
import com.jfinal.kit.PathKit;
/**
 * @author Joker
 * 水务集团的每日对账接口
 * 
 * 把当日所有的交易账单分别记录到一个文件中，再对账
 * 
 */
@Scheduled(cron = "0 0 0 * * ?")  //每天凌晨执行前一天
public class DailyCheckJob implements Job {
	
	private static Logger log = Logger.getLogger(DailyCheckJob.class);
	
	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		
		//缴费
		try {
			dealPay(1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//充值
		try {
			dealPay(2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		

	}
	
	/**
	 * 处理对账
	 * 
	 * @param type
	 * @throws FileNotFoundException
	 */
	private void dealPay(int type) throws FileNotFoundException{
		
		List<Payrecord> list = Payrecord.dao.getPayRecordListById(0,type);
		if(list.size()<=0){
			log.info("当日所有账单对账完毕"+System.currentTimeMillis());
			return;
		}
		//生成文件
		String pathName = getFileNameByType(type);
		
		try {
			File file = createAccountFile(type, pathName,list);
			//上传 文件
			boolean uploadFlag = FtpBusiness.uploadtoSWJT(file);
			
			if(!uploadFlag){
				log.debug("upLoad fail"+pathName);
				return;
			}
			
			//对账
			boolean checkingFlag = checking(type,pathName,Long.toString(file.length()));
			
			if(!checkingFlag){
				log.debug("checking fail"+pathName);
				return;
			}
			//更新数据库状态List ischK=1
			updateLocalData(list);
			
		} catch (IOException e) {
			log.debug("dealPay"+type+"异常");
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 *  更新数据库 对账完成的状态
	 * @param list
	 */
	private void updateLocalData(List<Payrecord> list){
		for(Payrecord payRecord:list){
			payRecord.set("isChk", 1).set("updateTime", new Date()).update();
		}
	}
	
	/**
	 * 创建文件
	 * 
	 * @param type
	 * @param pathName
	 * @param list
	 * @return
	 * @throws IOException
	 */
	public File createAccountFile(int type, String pathName,List<Payrecord> list) throws IOException{
		
		//创建文件
		String path = PathKit.getWebRootPath() + "/WEB-INF/classes/"+pathName;
		File filename = new File(path);
		FileOperation.creatTxtFile(filename);
		//写入文件
		String newStr = getStrOfOrder(type,list);
		FileOperation.writeTxtFile(newStr,"",filename);
		
		return filename;
	}
	
	/**
	 * 获得拼装文件的内容
	 * @param type
	 * @param list
	 * @return
	 */
	private String getStrOfOrder(int type,List<Payrecord> list){
		String temps="";
		// 交易日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String time = sdf.format(new Date());
		String realAddr = "WX";
		String  realWorkNo = "9994";
		int count =0;
		int fee=0;
		int total=0;
		for(Payrecord payRecord:list){
			String temp="";
			//实际付费
			float num= (float)payRecord.getFee()/100;   
			DecimalFormat df = new DecimalFormat("0.00");//格式化小数   
			String realPay = df.format(num);//返回的是String类型 
			
			if(type==1){		//缴费
				 temp = payRecord.getRemark() + "|" + time + "|" 
				+ payRecord.getRealNumber().toString() + "|" + realAddr 
				+ "|" + realPay+ "|" + realWorkNo+ "|" + payRecord.getFeeIDs() ;
			}else{		//type==2预存
			   temp = payRecord.getRemark() + "|" + time + "|" 
				+ payRecord.getRealNumber().toString() + "|" + realAddr 
				+ "|" + realPay+ "|" + realWorkNo+ "|"  ;
			}
			
			temps = temps+temp+"\r\n";
			fee=fee+payRecord.getFee();
			total= (int) (total+payRecord.getRealNumber());
			count++;
		}
		//实际付费
		float num= (float)fee/100;   
		DecimalFormat df = new DecimalFormat("0.00");//格式化小数   
		String totalFee = df.format(num);//返回的是String类型 
		
		temps+="0|"+time+"|"+count+"|"+totalFee+"|"+total;
		return temps;
	}
	
	/**
	 * 生成文件的名字
	 * 
	 * @param type
	 * @return
	 */
	private String getFileNameByType(int type){
		// 交易日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String time = sdf.format(new Date());
		// 银行代码
		String bankCode = "SZWX01";
		
		if(type==1){
			return bankCode+time+".DZ";
		}else{//type==2
			return bankCode+time+".YCDZ";
		}
	}

	/**
	 * 调用对账接口 
	 * @param payRecord
	 * 
	 * 	拼接文件 ,然后调用接口
	 * 
	 * @return boolean
	 */
	public boolean checking(int type,String fileName,String length){
		
		//支付日期
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String accountDate = sdf.format(new Date());
		try {
			if(type==1){
				YouPuSocketService.getChkAccountDataForPay(accountDate, fileName, length);
			}else{//type==2
				YouPuSocketService.getChkAccountDataForrePay(accountDate, fileName, length);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	
	}
	
}
