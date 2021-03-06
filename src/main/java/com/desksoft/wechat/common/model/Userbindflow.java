package com.desksoft.wechat.common.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.desksoft.wechat.common.model.base.BaseUserbindflow;
import com.desksoft.wechat.service.StrSQLService;
import com.jfinal.plugin.activerecord.Page;

@SuppressWarnings("serial")
public class Userbindflow extends BaseUserbindflow<Userbindflow> {
	
	public static final Userbindflow me = new Userbindflow();
	public static final int IS_DELETE_NO = 0;
	
	public Page<Userbindflow> getBindingUserPaginateList(int pageNumber, int pageSize,Date startDate,Date endDate,int userNo) {
		String SQL = "from userbindflow where isdelete = 1 and isBinding=1";
		SQL = StrSQLService.getSQLByDatePorid(startDate, endDate, SQL);
		if(userNo>0){
			SQL += " and userNo =" + userNo  ;
		}
		SQL +=" order by id asc";
		return paginate(pageNumber, pageSize, "select *", SQL);
	}
	
	public boolean delBindingUser(Object idValue){
		return me.findById(idValue).set("isDelete", IS_DELETE_NO).update();
	}
	
	public boolean isBindingUser(String openid){
		if(StringUtils.isNotBlank(openid)){
			List<Userbindflow> list = selectBindingUserList(openid);
			if(list.size()>0){
				return true;
			}
		}	
		return false;
	}
	
	public boolean isBindingByOpenIdAndUserNo(String openID,String userNo){
		Userbindflow exis = me.findFirst("select * from userbindflow where openid=? and userNo=?", openID, userNo );
		if(exis==null){
			return false;
		}else{
			exis.set("isBinding", 1).set("updateTime", new Date()).update();
			return true;
		}
	}
	
	
	/**
	 * 该微信号绑定的有效户号
	 * @param openid
	 * @return
	 */
	public List<Userbindflow> selectBindingUserList(String openid){
		List<Userbindflow> list = me.find("select * from userbindflow where openID=? and isdelete=1 and isBinding=1", openid);
		return list;
	}
	
	/**
	 * 微信端添加绑定记录
	 * @param openID
	 * @param userNo
	 * @param mobileno
	 * @return 1绑定成功2重新绑定成功0绑定失败 -1已经绑定过
	 */
	public int addBindUser(String openID,Integer userNo,String mobileno,String address,String userName){
		boolean flag = true;
		Userbindflow exis = me.findFirst("select * from userbindflow where openid=? and userNo=?", openID, userNo );
		if(exis==null){
			flag = new Userbindflow().set("openID",openID).
					set("userNo", userNo). 
					set("phoneNum", mobileno).
					set("address", address). 
					set("userName", userName).
					set("createTime", new Date()).
					set("updateTime", new Date()).save();
			if(flag){
				return 1;
			}else{
				return 0;
			}
		}else{
			if(0==exis.getIsBinding()||0==exis.getIsDelete()){
				flag = exis.set("isBinding", 1).set("phoneNum", mobileno).set("isdelete", 1).set("updateTime", new Date()).update();
				if(flag){
					return 2;
				}else{
					return 0;
				}
			}else{
				return -1;
			}
		}
		
	}
	
	/**
	 * 解除绑定
	 * @param openId
	 * @param userNo
	 * @return
	 */
	public boolean unbindSingleUser(String openId,Integer userNo){
		boolean flag = true;
		Userbindflow exis = me.findFirst("select * from userbindflow where openid=? and userNo=?", openId, userNo );
		if(exis!=null){
			flag = exis.set("isBinding", 0).update();
		}
		return flag;
	}
	
	/**
	 * @param id
	 * @return List<UserBindFlow>
	 *  获得所有绑定的(户号) 关注的(微信号) 未删除的 
	 */
	public List<Userbindflow> getUserBindFlowListById(){
		return getUserBindFlowListById(0);
	}
	
	/**
	 * @param id
	 * @return List<UserBindFlow>
	 *  获得所有绑定的(户号) 关注的(微信号) 未删除的 
	 */
	public List<Userbindflow> getUserBindFlowListById(int miniID){
		return getUserBindFlowListById(miniID,0,10);
	}
	
	/**
	 * @param miniID
	 * @param index
	 * @param num
	 * @return   获得所有绑定的(户号) 关注的(微信号) 未删除的 
	 */
	public List<Userbindflow> getUserBindFlowListById(int miniID,int index,int num){
		//查询前一天	
		 String sql = "select * from userbindflow inner join user where  userbindflow.openID=user.openid and user.isFollow=1 and userbindflow.isdelete=1 and userbindflow.isBinding=1 and userbindflow.id > ? limit ?,?"; 
		 List<Userbindflow> list = me.find(sql,miniID,index,num);
		 return list;
	}
	
}
