
//ページ読み込みイベントに登録
document.addEventListener("DOMContentLoaded", System, false);
//カレンダー作成
function createCalendar(ServletURL) {
	var mCalendar = WM.createCalendarView();
	mCalendar.setClientStyle(WM.STYLE_TOP);

	//月変更イベント処理
	mCalendar.addEvent("onDate", onDate);
	function onDate() {
		var date = mCalendar.getDate();
		date.setDate(1);
		System.loadDiary();
	}
	//日付イベント処理
	mCalendar.addEvent("onDay", onDay);
	function onDay(params) {
		var date = params["date"];
		//location.search = AFL.sprintf("p=%d-%d-%d", date.getFullYear(), date.getMonth() + 1, date.getDate());
		history.pushState(null, "", "?p=" + AFL.sprintf("%d-%d-%d", date.getFullYear(), date.getMonth() + 1, date.getDate()));
		mCalendar.setDate(date);
    }
	//カレンダー直接操作時の月変更イベント
	mCalendar.addEvent("onChange", onChange);
	function onChange(params) {
		var date = params["date"];
		history.pushState(null, "", "?p=" + AFL.sprintf("%d-%d-%d", date.getFullYear(), date.getMonth() + 1, date.getDate()));
	}
	return mCalendar;
}
function createSearchView(diaryManager)
{
	var mFrame = WM.createFrameWindow();
	mFrame.setTitle("検索");
	mFrame.setSize(500,300);

	var mPanel = WM.createPanel();
	mPanel.setClientStyle(WM.STYLE_TOP);
	mFrame.addChild(mPanel);

	var mButtonSet = WM.createButton();
	mButtonSet.setText("検索");
	mButtonSet.setClientStyle(WM.STYLE_LEFT);
	mPanel.addChild(mButtonSet);
	mButtonSet.onclick = function()
	{
		function onSearch(params)
		{
			mListView.clear();
			var diaryList = params["diary"];
			for (var index in diaryList) {
				var diary = diaryList[index];
				var date = new Date(diary["diaryDate"]);
				var index = mListView.addItem(AFL.sprintf("%d年%d月%d日", date.getFullYear(), date.getMonth()+1, date.getDate()));
				mListView.setItem(index, 1, diary["diaryTitle"]);
				mListView.setItemValue(index, diary["diaryId"]);
			}
		}
		diaryManager.searchDiary(mTextView.getText(),onSearch);
	}

	var mTextView = WM.createTextBox();
	mTextView.setBackgroundColor(0xe0e0e0e0);
	mTextView.setClientStyle(WM.STYLE_CLIENT);
	mPanel.addChild(mTextView);

	var mListView = WM.createListView();
	mListView.setClientStyle(WM.STYLE_CLIENT);
	mListView.setBackgroundColor(0xc0ffffff);
	mFrame.addChild(mListView);
	mListView.addHeader("日付");
	mListView.addHeader("タイトル");
	mListView.autoWidth();
	mListView.addEvent("onItemClick", onItemClick);
	function onItemClick(param)
	{
		var id = mListView.getItemValue(param.index);
		System.drawDiaryById(id);
	}

	mFrame.setPos();
	mTextView.setFocus();
	return mFrame
}

//日記システム総合処理
function System() {
	//ロケーション情報から表示内容を確認
	function goLocation() {
		var hash = location.search;
		if (/\?p=\d+-\d+-\d+/.test(hash)) {
			//日付設定
			mCalendar.setDate(new Date(hash.substr(3).replace(/-/g, "/")));
		}
		else if (/\?p=\d+-\d+/.test(hash)) {
		    //日付設定
		    mCalendar.setDate(new Date(hash.substr(3).replace(/-/g, "/")+"/1"));
		}
		else if (/\?p=\d/.test(hash)) {
			//ID指定
			System.drawDiaryById(parseInt(hash.substr(3)));
		}
		else {
			//最新のデータを表示
			System.drawNewDiary();
		}
	}
	//ブラウザの戻るボタンなどでアドレスが変更された場合の処理
	window.addEventListener('popstate', goLocation, false);

	//ログインイベント発生時に呼ばれる
	function onLogin(param) {
		//管理ユーザかどうかで表示内容を変更
		var flag = param != null;
		mButtonUser.setText(flag ? param : "未ログイン");
		mButtonUserEdit.setVisible(flag);
		mButtonEdit.setVisible(flag);
		mButtonInfo.setVisible(flag);
		mButtonFile.setVisible(flag);
		goLocation();
		System.drawInfo();
	}
	//管理ユーザか返す
	System.isAdmin = function () {
		return mUserManager.isAdmin();
	}
	//日記編集機能の呼び出し
	System.editDiary = function (id) {
		var messageView = WM.createMessageView("編集画面構築中");
		setTimeout(function () {
			var edit = WM.createEditFrame(ServletURL);
			messageView.close();
			if (id)
				edit.load(id);
		}, 10);

	}
	//ID指定による日記の描画処理
	System.drawDiaryById = function (id) {
		function onDate(params) {
			var date = params["date"];
			if (date)
				mCalendar.changeDate(new Date(date));
			else
				mCalendar.changeDate(new Date());
		}
		//ID指定
		mDiaryManager.getDate(id, onDate);
	}
	//最新の日記の描画処理
	System.drawNewDiary = function () {
		function onDate(params) {
			var date = null;
			if (params)
				date = params["date"];
			mCalendar.setDate(new Date(date));
			history.pushState(null, "", ".");
		}
		mDiaryManager.getLastDate(onDate);
	}
	//カレンダーの範囲の日記を呼び出す
	System.loadDiary = function () {
		function onDiaryLoad(params) {
			if (params) {
				mCalendar.clearDateColor();
				var date = mCalendar.getDate();
				var listDiary = params["diary"];
				for (var index in listDiary) {
					var diary = listDiary[index];
					var diaryDate = new Date(diary["diaryDate"]);
					mCalendar.setDateColor(diaryDate, 0xdd88ff88);

					if (diaryDate.getFullYear() != date.getFullYear() || diaryDate.getMonth() != date.getMonth())
						diary["enable"] = false;
					else
						diary["enable"] = true;
				}
				System.drawTitle(params);
				mDiaryView.drawDiary(params);
				mMessageView.close();
			}
			else
				mMessageView.setText("読み込み失敗");
		}
		var mMessageView = WM.createMessageView("読み込み中");
		mDiaryManager.getDiary(null, mCalendar.getDateStart(), mCalendar.getDateEnd(), onDiaryLoad);
	}
	//インフォメーションメッセージの描画
	System.drawInfo = function () {
		function onDiaryLoad(params) {
			if (params)
			{
				var listDiary = params["diary"];
				var diary = listDiary[0];
				if (diary) {
					var s =
						"<DIV style='background-color:#FFEEEE;padding:0.2em;'>" +
						"<DIV style='background-color:#FFFFEE'>" + diary["diaryMessage"] + "</DIV>" +
						"</DIV>";
					mInfoMessage.innerHTML = s;
				}
			}

		}
		mDiaryManager.getDiary(1, null, null, onDiaryLoad);
	}
	System.drawTitle = function (params) {
	    var s = "<DIV style='background-color:#FFEEEE;padding:0.2em;'>" +
				"<DIV style='background-color:#FFFFEE'>";

	    var listDiary = params["diary"];
	    for (var index in listDiary) {
	        var diary = listDiary[index];
	        var d = new Date(diary["diaryDate"]);
	        s += AFL.sprintf(
                "<DIV style='margin:3px;white-space:nowrap;'>" +
                "<A style='text-decoration: none;' href='?p=%d-%d-%d'>"+
                "<SPAN style='color:green;'>%d/%02d/%02d</SPAN> "+
                "<SPAN style='color:black;'>%s</SPAN></A>"+
                "</DIV>",
                d.getFullYear(), d.getMonth() + 1, d.getDate(), d.getFullYear(), d.getMonth() + 1, d.getDate(),diary["diaryTitle"]);
	    }
	    s += "</DIV></DIV>";
        mInfoLink.innerHTML = s;
	}
	//サーブレットURL設定でテスト用と本番アドレスを分ける
	var ServletURL;
	if (location.port == "8080")
		ServletURL = AFL.sprintf("http://%s:8080/diary/", location.hostname);
	else
		ServletURL = AFL.sprintf("http://%s/", location.hostname);
	//各機能ごとのマネージャを作成
	var mDiaryManager = AFL.createDiaryManager(ServletURL);
	var mUserManager = AFL.createUserManager(ServletURL);
	//画面分割ウインドウの作成
	var split = WM.createSplit();
	split.setClientStyle(WM.STYLE_CLIENT);
	split.setBarPos(275);
	//サイドビューの作成
	var mSideView = WM.createWindow();
	mSideView.setClientStyle(WM.STYLE_CLIENT);
	split.addChild(mSideView);
	//最新の日記ボタンの作成
	var mButtonNew = WM.createButton();
	mButtonNew.setClientStyle(WM.STYLE_TOP);
	mButtonNew.setText("最新の日記");
	mSideView.addChild(mButtonNew);
	mButtonNew.onclick = function () {
		System.drawNewDiary();
	}
	//カレンダーの作成
	var mCalendar = createCalendar(ServletURL);
	mSideView.addChild(mCalendar);
	//検索ボタンの作成
	var mButtonSearch = WM.createButton();
	mButtonSearch.setClientStyle(WM.STYLE_TOP);
	mButtonSearch.setText("検索");
	mSideView.addChild(mButtonSearch);
	mButtonSearch.onclick = function () {
		createSearchView(mDiaryManager);
	}

	//インフォメーションビューの作成
	var mInfoView = WM.createWindow();
	mInfoView.setClientStyle(WM.STYLE_CLIENT);
	mInfoView.setScroll(true);
	mSideView.addChild(mInfoView);

	var mInfoMessage = document.createElement("div");
	mInfoView.appendChild(mInfoMessage);
	var mInfoLink = document.createElement("div");
	mInfoView.appendChild(mInfoLink);

	//ログインボタンの作成
	var mButtonUser = WM.createButton();
	mButtonUser.setClientStyle(WM.STYLE_BOTTOM);
	mButtonUser.setText("ログイン");
	mSideView.addChild(mButtonUser);
	mButtonUser.onclick = function () {
		//ログインウインドウの呼び出し
		var loginWindow = WM.createLoginWindow(ServletURL, onLogin);
		loginWindow.setPos();
	}
	//ユーザ編集ボタンの作成
	var mButtonUserEdit = WM.createButton();
	mButtonUserEdit.setClientStyle(WM.STYLE_BOTTOM);
	mButtonUserEdit.setText("ユーザ編集");
	mButtonUserEdit.setVisible(false);
	mSideView.addChild(mButtonUserEdit);
	mButtonUserEdit.onclick = function () {
		//ユーザ編集ウインドウの呼び出し
		var userListWindow = WM.createUserListWindow(ServletURL);
		userListWindow.setPos();
	}
	//ファイル編集ボタンの作成
	var mButtonFile = WM.createButton();
	mButtonFile.setClientStyle(WM.STYLE_BOTTOM);
	mButtonFile.setText("ファイル");
	mButtonFile.setVisible(false);
	mSideView.addChild(mButtonFile);
	mButtonFile.onclick = function () {
		//ファイル編集ウインドウの呼び出し
		WM.createFileView(ServletURL);
	}
	//インフォメーション編集ボタンの作成
	var mButtonInfo = WM.createButton();
	mButtonInfo.setClientStyle(WM.STYLE_BOTTOM);
	mButtonInfo.setText("メッセージ");
	mButtonInfo.setVisible(false);
	mSideView.addChild(mButtonInfo);
	mButtonInfo.onclick = function () {
		//インフォメーション編集ウインドウの呼び出し
		System.editDiary(1);
	}
	//日記の新規作成ボタンの作成
	var mButtonEdit = WM.createButton();
	mButtonEdit.setVisible(false);
	mButtonEdit.setClientStyle(WM.STYLE_BOTTOM);
	mButtonEdit.setText("新規作成");
	mSideView.addChild(mButtonEdit);
	mButtonEdit.onclick = function () {
		//日記の新規作成
		System.editDiary(0);
	}
	//日記表示用ビューの作成
	var mDiaryView = WM.createDiaryView(ServletURL);
	split.addChild(mDiaryView);
	//サーバにセッション情報の問い合わせを行う
	mUserManager.callSession(onLogin);

}
