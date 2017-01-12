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
if(emailId){
	userIdExists = ec.entity.find("moqui.security.UserAccount").condition('username',emailId).count();
	if(userIdExists>0){
		ec.message.addError("User already exists with this email");
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
requestJson.requestInfo.roleTypeId=roleTypeId;
requestJson.requestInfo.organizationId=organizationId;
JsonBuilder jb = new JsonBuilder();
jb.call(requestJson);
requestJsonStr = jb.toString();
if(ec.message.getErrors().size()>0){
	ec.web.sendJsonResponse(ec.message);
	return;
}
