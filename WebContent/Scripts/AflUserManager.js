(function () {
	AFL.createUserManager = function (ServletURL) {
		var MANAGER_URL = ServletURL + "UserManager"
		var manager = {};
		manager.login = function (user, pass, func) {
			AFL.sendJson(MANAGER_URL + "?cmd=user_login",
				{ "user": user, "pass": pass },
				function (data) {
				    if (data && data["user_session"])
					{
						var session = data["user_session"];
						var name = data["user_name"];
						localStorage.setItem(ServletURL+"user_session", session);
						sessionStorage.setItem("Session", session);
						sessionStorage.setItem("user_name", name);
						func(name);
					}
					else
					{
					    sessionStorage.removeItem("Session");
					    sessionStorage.removeItem("user_name");
					    func(null);
					}
				}
			);
		}
		manager.logout = function () {
		    localStorage.removeItem(ServletURL + "user_session");
		    sessionStorage.removeItem("Session");
		    sessionStorage.removeItem("user_name");
		}
		manager.session = function (session, func) {
			AFL.sendJson(MANAGER_URL + "?cmd=user_session",
					{ "session": session }, func);
		}
		manager.callSession = function (func) {
		    function onSession(data)
		    {
		        var userSession = data["user_session"];
		        if (userSession) {
		            localStorage.setItem(ServletURL + "user_session", userSession);
		            sessionStorage.setItem("Session", userSession);
		        }
		        else {
		            localStorage.removeItem(ServletURL + "user_session");
		            sessionStorage.removeItem("Session");
		        }
		        var userName = data["user_name"];
		        if (userName) {
		            sessionStorage.setItem("user_name", userName);
		        }
		        func(userName);
		    }
			var session = localStorage.getItem(ServletURL + "user_session");
			if (session) {
				this.session(session,onSession);
			}
			else {
				func(null);
			}
		}
		manager.getUserList = function(func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=user_list",
					{}, func);
		}
		manager.setUser = function(id,enable,name,pass,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=user_set",
					{"user_id":id,"user_enable":enable,"user_name":name,"user_pass":pass}, func);
		}
		manager.delUser = function (id,func) {
			AFL.sendJson(MANAGER_URL + "?cmd=user_del",{ "user_id": id}, func);
		}
		manager.isAdmin = function()
		{
			if (sessionStorage.getItem("Session") != null)
				return true;
			return false;
		}
		return manager;
	}
	WM.createUserListWindow = function (ServletURL)
	{
		var mManager = AFL.createUserManager(ServletURL);
		var frame = WM.createFrameWindow();
		frame.setBackgroundColor(0xeeeeeeff);
		frame.setSize(640, 400);

		var split = WM.createSplit();
		frame.addChild(split);
		split.setBarPos(300);

		//-------------------
		//ユーザ情報編集
		var userWindow = WM.createWindow();
		userWindow.setClientStyle(WM.STYLE_CLIENT);
		split.addChild(userWindow,1);
		var input = document.createElement("div");
		input.innerHTML =
			"<div style='margin:1em;width:18em;'>" +
			"<button style='cursor:pointer;display:inline-block;width:13em;margin:0.4em;padding:0.1em;background-color:#eeeeee' class='user_set'>設定</button>" +
			"<button style='cursor:pointer;display:inline-block;margin:0.4em;padding:0.1em;background-color:#eeeeee' class='user_del'>削除</button><BR>" +
			"<div style='background-color:#FFFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>ID</span><input class='user_id' disabled style='opacity:0.7;width:10em'></div>" +
			"<div style='background-color:#AAFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>有効</span><input class='user_enable' type='checkbox' style='opacity:0.7;'></div>" +
			"<div style='background-color:#FFFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>ユーザ名</span><input class='user_name' style='opacity:0.7;width:10em'></div>" +
			"<div style='background-color:#AAFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>パスワード</span><input class='user_pass' type='password' style='opacity:0.7;width:10em'></div>" +
			"</div>";
		userWindow.setHtml(input);
		var inputId = input.getElementsByClassName("user_id")[0];
		var inputEnable = input.getElementsByClassName("user_enable")[0];
		var inputName = input.getElementsByClassName("user_name")[0];
		var inputPass = input.getElementsByClassName("user_pass")[0];
		var buttonSet = input.getElementsByClassName("user_set")[0];
		var buttonDel = input.getElementsByClassName("user_del")[0];
		buttonSet.onclick = function()
		{
			var id = parseInt(inputId.value);
			if (isNaN(id))
				id = 0;
			mManager.setUser(id, inputEnable.checked, inputName.value, inputPass.value, function () { frame.load(); });
		}
		buttonDel.onclick = function () {
			var id = parseInt(inputId.value);
			mManager.delUser(id, function () { frame.load(); });
		}
		//-------------------


		var mList = WM.createListView();
		mList.setClientStyle(WM.STYLE_CLIENT);
		split.addChild(mList);
		mList.addHeader("ID");
		mList.addHeader("ENABLE");
		mList.addHeader("NAME");
		mList.setHeaderWidth(0, 32);
		mList.setHeaderWidth(1, 80);
		mList.setHeaderWidth(2, 256);

		function onItemClick(param)
		{
			var index = param["index"];
			var userData = mList.getItemValue(index);
			if (userData)
			{
				inputId.value = userData["userId"];
				inputEnable.checked = userData["userEnable"];
				inputName.value = userData["userName"];
				inputPass.value = "*";
			}
			else
			{
				inputId.value = "-";
				inputEnable.checked = true;
				inputName.value = "";
				inputPass.value = "";
			}


		}
		mList.addEvent("onItemClick", onItemClick);

		function onLoad(data)
		{
			mList.clear();
			var list = data["user_list"];
			for(var index in list)
			{
				var userData = list[index];
				var item = mList.addItem(userData["userId"]);
				mList.setItemValue(item,userData);
				mList.setItem(item, 1, userData["userEnable"]?"有効":"無効");
				mList.setItem(item, 2, userData["userName"]);
			}
			var item = mList.addItem("-");
			mList.setItem(item, 1, "未設定");
			mList.setItem(item, 2, "新規");
			mList.setItemValue(item, null);
		}

		frame.load = function()
		{
			mManager.getUserList(onLoad);
		}
		frame.load();
		frame.setPos();
		return frame;
	}
	WM.createLoginWindow = function (ServletURL,funcLogin) {
		var mManager = AFL.createUserManager(ServletURL);
		var frame = WM.createFrameWindow();
		frame.setSize(300, 160);
		frame.setBackgroundColor(0xeeeeeeff);

		
		var inputUser = document.createElement("input");
		var input = document.createElement("div");
		input.innerHTML = 
			"<div style='margin:1em;width:18em;text-align:center'>"+
			"<div style='background-color:#FFFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>ユーザ名</span><input class='name' style='opacity:0.7;width:128px'></div>" +
			"<div style='background-color:#AAFFAA'><span style='display:inline-block;padding:0.4em;width:6em'>パスワード</span><input class='pass' type='password' style='opacity:0.7;width:128px'></div>" +
			"<button style='cursor:pointer;display:inline-block;margin:0.2em;padding:0.1em;background-color:#eeeeee' class='login'>ログイン</button>" +
			"<button style='cursor:pointer;display:inline-block;margin:0.2em;padding:0.1em;background-color:#eeeeee' class='logout'>ログアウト</button><BR>" +
			"<SPAN class='msg'></SPAN>"+
			"</div>";
		frame.setHtml(input);
		var inputName = input.getElementsByClassName("name")[0];
		var inputPass = input.getElementsByClassName("pass")[0];
		var buttonLogin = input.getElementsByClassName("login")[0];
		var buttonLogout = input.getElementsByClassName("logout")[0];
		var message = input.getElementsByClassName("msg")[0];
		inputName.focus();
		buttonLogin.onclick = function ()
		{
			message.innerHTML = "ログイン中";
			mManager.login(inputName.value, inputPass.value, onLogin);
		}
		buttonLogout.onclick = function()
		{
			mManager.logout();
			message.innerHTML = "ログアウト";
			if (funcLogin)
				funcLogin(null);
		}

		function onLogin(name)
		{
			if (name != null) {
				message.innerHTML = name;
				frame.close();
			} else
				message.innerHTML = "ログイン失敗";
			if (funcLogin)
				funcLogin(name);
		}
		//mManager.callSession(onLogin);
		return frame;
	}

})();