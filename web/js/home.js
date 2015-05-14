// JavaScript Document
$(document).ready(function() {
	//ajaxFillAmiibos();
	setClickables();
	ajaxFillAmiiboList();
});

var root = "http://localhost:8080";

var setClickables = function(){
	$("#submitNewAmiibo").click(ajaxNewAmiibo);
	$("#submitEditAmiibo").click(ajaxEditAmiibo);
	$("#submitDeleteAmiibo").click(ajaxDeleteAmiibo);
	$("#getTotal").click(ajaxGetAmiiboTotal);
}

var prepareEdit = function(){
	var name = $(this).parent().siblings("div.amiibo-name").text();
	var ownership = $(this).parent().siblings("div.amiibo-ownership").text();
	var price = $(this).parent().siblings("div.amiibo-price").text();
	
	$("#editAmiiboName").val(name);
	if (ownership != "undefined"){
		$("#editAmiiboOwnership").val(ownership);
	}
	if (price != "undefined") {
		$("#editAmiiboPrice").val(price);
	}
	var index = $(this).parent().parent().index();
	$("#editAmiiboModal").attr("name", index);
}

var prepareDelete = function(){
	var name = $(this).parent().parent().find(".amiibo-name").html();
	$("#deleteAmiiboModal").attr("name", name);
}

var fillAmiibos = function(data){
	$(".fill-amiibos").empty();
	var data = data.collection
	
	var length = data.length;
	for (var i = 0; i < length; i++){
		var name = data[i].name;
		var ownership = data[i].ownership;
		var price = data[i].price;
		$(".fill-amiibos").append("<div class=\"amiibo-cell\"><div class=\"amiibo-name\">"+ name +"</div><div class=\"amiibo-ownership\">"+ownership+"</div><div class=\"amiibo-price\">"+price+"</div><div><div class=\"amiibo-edit\" data-toggle=\"modal\" data-target=\"#editAmiiboModal\"></div><div class=\"amiibo-delete\" data-toggle=\"modal\" data-target=\"#deleteAmiiboModal\"></div></div></div>");
	}
	
	$(".amiibo-edit").hover(editHoverOn, editHoverOff);
	$(".amiibo-edit").click(prepareEdit);
	$(".amiibo-delete").hover(deleteHoverOn, deleteHoverOff);
	$(".amiibo-delete").click(prepareDelete);
}

var fillTotal = function(data){
	$("#totalAmiiboCount").html(data.amiibos);
}

var editHoverOn = function() {
	$(this).css("background-image","url(images/edit_hover.png)");
}

var editHoverOff = function() {
	$(this).css("background-image","url(images/edit.png)");
}

var deleteHoverOn = function() {
	$(this).css("background-image","url(images/delete_hover.png)");
}

var deleteHoverOff = function() {
	$(this).css("background-image","url(images/delete.png)");
}

var ajaxFillAmiiboList = function() {
	$.ajax({
		url: root+'/ServletAmiibo/GetServlet',
		type: 'GET',
		contentType:"application/json",
		dataType:"json",
		success: function(data){setTimeout(fillAmiibos,800,data)}
	});
}

var ajaxGetAmiiboTotal = function() {
	$.ajax({
		url: root+'/ServletAmiibo/GetServletTotal',
		type: 'GET',
		contentType:"application/json",
		dataType:"json",
		success: function(data){setTimeout(fillTotal,800,data)}
	});
}

var ajaxNewAmiibo = function() {
	$('#newAmiiboModal').modal('hide');
	var name = $("#newAmiiboName").val();
	var ownership = $("#newAmiiboOwnership").val();
	var price = $("#newAmiiboPrice").val();
	$.ajax({
		url: root+'/ServletAmiibo/PostServlet',
		type: 'POST',
		contentType:"application/json",
		data: JSON.stringify({
			name: name,
			ownership: ownership,
			price: price
		}),
		success: function(){setTimeout(newAmiiboCallback,800)},
		error: function(error){console.log(error)}
	});
}

var newAmiiboCallback = function(){
	$("#newAmiiboName").val("");
	$("#newAmiiboPrice").val("");
	ajaxFillAmiiboList();
}

var ajaxEditAmiibo = function() {
	$('#editAmiiboModal').modal('hide');
	var name = $("#editAmiiboName").val();
	var ownership = $("#editAmiiboOwnership").val();
	var price = $("#editAmiiboPrice").val();
	$.ajax({
		url: root+'/ServletAmiibo/PutServlet',
		type: 'PUT',
		contentType:"application/json",
		data: JSON.stringify({
			name: name,
			ownership: ownership,
			price: price
		}),
		success: function(){setTimeout(editAmiiboCallback,800)},
		error: function(error){console.log(error)}
	});
}

var editAmiiboCallback = function(){
	$("#editAmiiboName").val("");
	$("#editAmiiboPrice").val("");
	ajaxFillAmiiboList();
}

var ajaxDeleteAmiibo = function() {
	$('#deleteAmiiboModal').modal('hide');
	var name = $("#deleteAmiiboModal").attr("name");
	$.ajax({
		url: root+'/ServletAmiibo/DeleteServlet/'+name,
		type: 'DELETE',
		success: function(){setTimeout(deleteAmiiboCallback,1100)},
		error: function(error){console.log(error)}
	});
}

var deleteAmiiboCallback = function(){
	ajaxFillAmiiboList();
}