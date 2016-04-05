
$(function(){
    $('input:checkbox').on('change', function(){
        if($('input:checkbox:checked').val()) {
            $("#fsubmit").css('display','block');
        }else{
            $("#fsubmit").css('display','none');
        }
    })
    $("#fsubmit").click(function() {submit()})
});
function submit(){
	
	var url = decodeURI(location.href);
	var index = url.indexOf("/bindingUser");
	var href = url.substring(0,index);
	  var openId = $.trim($('#openId').val());
	  if(checkOpenID(openId)){
		  var userno = $.trim($('#userno').val());
		  var mobileno = $.trim($('#phoneNum').val());
		  var userName = $.trim($('#userName').val());
		  var address = $.trim($('#address').val());
		  var refeer = $.trim($('#tjCode').val());
    	  var d ={
    			  "openId":openId,
       			  "userno":userno,
       			  "userName":userName,
       			 "address":address,
       			  "mobileno":mobileno,
       			"refeer":refeer
       			 }
    	  $.ajax({
    		  	// dataType : "json",
                 type : 'post',
                 data: d,
                 url : href+"/bindingUser/bind/json",
                 success : function(obj) {
                     if(true==obj.flag){
                    	 $(".scuess").show();
                    	 $(".panel-body").hide()
                     }else{
                    	 alert(obj.msg);
                     }
                 },
                 failure:function (result) {  
                     alert('提交失败，请重新提交');  
                  }, 
             });  
      }
  }
function checkOpenID(openId){
	 if(null==openId || ""==openId) {
		 alert("无法获取openID,请刷新页面");
        return false;
	 }
	 return true;
}

