import groovy.json.JsonBuilder;

requestJson = [:];
requestJson.requestType=requestType;
requestJson.requestInfo=[:];
requiredFields = ["firstName":"First Name","lastName":"Last Name","productStoreId":"Store","phone":"Phone","emailId":"Email","aadharIdPath":"Aadhar","userPhoto":"Photo","addressIdPath":"Address Identification"]
requiredFields.keySet().each {
	if(!context.get(it)){
		ec.message.addError(requiredFields.get(it)+" cannot be empty");
	}
}
requestJson.requestInfo.firstName=firstName;
requestJson.requestInfo.lastName=lastName;
requestJson.requestInfo.productStoreId=productStoreId;
requestJson.requestInfo.phone=phone;
requestJson.requestInfo.emailId=emailId;
requestJson.requestInfo.address=address;
requestJson.requestInfo.weeklyOff=weeklyOff;
requestJson.requestInfo.aadharIdPath=aadharIdPath;
requestJson.requestInfo.userPhoto=userPhoto;
requestJson.requestInfo.addressIdPath=addressIdPath;
JsonBuilder jb = new JsonBuilder();
jb.call(requestJson);
requestJsonStr = jb.toString();
if(ec.message.getErrors().size()>0){
	ec.web.sendJsonResponse(ec.message);
	return;
}
