
//获取url参数
function getQueryVariable(variable) {
	var query = window.location.search.substring(1);
	var vars = query.split("&");
	for (var i = 0; i < vars.length; i++) {
		var pair = vars[i].split("=");
		if (pair[0] == variable) {
			return "./" + pair[1];
		}
	}
	//为空情况
	return ("./README.md");
}

//获取文章名
function getTitle(variable) {
	return variable.substring(variable.lastIndexOf("/") + 1, variable.lastIndexOf(".md"));
}

//获取文章名
var artUrl = decodeURIComponent(getQueryVariable("art"));

window.onload = function () {
	//获取文章markdown文件
	$.ajax({
		url: artUrl,
		type: "get",
	}).done(function (output) {
		//解析文章并填充文章名以及文章
		var converter = new showdown.Converter({
			tables: true
		});
		if (artUrl == "./README.md") {
			$("#articleTitle").html("Scrcpy");
		} else {
			$("#articleTitle").html(getTitle(artUrl));
		}
		$("#articleBody").html(converter.makeHtml(output));
		directory();
		hljs.highlightAll();
	}).fail(function () {
		$.ajax({
			url: "./README.md",
			type: "get",
		}).done(function (output) {
			var converter = new showdown.Converter();
			$("#articleTitle").html("Scrcpy");
			$("#articleBody").html(converter.makeHtml(output));
			directory();
		}).fail(function () {
			console.log("error,can not find article!");
		});
	});
}

//自动生成目录
function directory() {
	$("#articleBody").find("h1,h2,h3,h4,h5,h6").each(function (i, item) {
		var tag = $(item).get(0).localName;
		$(item).attr("id", "p" + i);
		$("#articleContent").append('<p class = "contentTitle"><a class = "contentTitle-' + tag + '" onclick="GoTo(\'#p' + i + '\')" >' + $(this).text() + '</a></p>');
	});
	$(".contentTitle-h1").css({
		"margin-left": "0px",
		"font-size": "18px",
		"line-height": "22px",
		"height": "22px"
	});
	$(".contentTitle-h2").css({
		"margin-left": "30px",
		"font-size": "16px",
		"line-height": "20px",
		"height": "20px"
	});
	$(".contentTitle-h3").css({
		"margin-left": "60px",
		"font-size": "14px",
		"line-height": "18px",
		"height": "18px"
	});
	$(".contentTitle-h4").css({
		"margin-left": "90px",
		"font-size": "12px",
		"line-height": "16px",
		"height": "16px"
	});
	$(".contentTitle-h5").css({
		"margin-left": "120px",
		"font-size": "10px",
		"line-height": "14px",
		"height": "14px"
	});
	$(".contentTitle-h6").css({
		"margin-left": "150px",
		"font-size": "8px",
		"line-height": "12px",
		"height": "12px"
	});
}

//点击目录滚动到对应位置
function GoTo(link) {
	$("html,body").animate({
		scrollTop: $(link).offset().top
	}, 400);
}