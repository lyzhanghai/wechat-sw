<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
	<head>
		<meta charset="utf-8">
		<title>水务缴费/预存</title>
	</head>
<body onload="closeWindow();">
	<h1>支付成功</h1>
	 <div id="show">  
  将倒计时10秒后关闭当前窗口，返回微信公众号窗口
</div>
</body>
<script type="text/javascript">
var time=5;  
function closeWindow(){  
	window.setTimeout('closeWindow()',1000);  
	if(time>0){  
		document.getElementById("show").innerHTML=" 将倒计时<font color=red>"+time+"</font>秒后关闭当前窗口,返回微信公众号窗口";  
		time--;  
	}else{  
		WeixinJSBridge.call('closeWindow');
		//this.window.opener=null; //关闭窗口时不出现提示窗口  
		//window.close();  
	}
}
</script>
</html>