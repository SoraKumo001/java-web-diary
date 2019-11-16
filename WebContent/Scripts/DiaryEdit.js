(function () {
	AFL.createDiaryManager = function (ServletURL) {
		var MANAGER_URL = ServletURL + "DiaryManager"
		var mDiaryManager = {};
		mDiaryManager.writeDiary = function (id, title, message, date, visible, func) {
			var params =
			{
				"id": id,
				"visible": visible,
				"date": AFL.sprintf("%d/%d/%d", date.getFullYear(), date.getMonth() + 1, date.getDate()),
				"title": title,
				"message": message
			};
			AFL.sendJson(MANAGER_URL + "?cmd=diary_write", params, func);
		}
		mDiaryManager.delDiary = function (id,func) {
			var params =
			{
				"id": id
			};
			AFL.sendJson(MANAGER_URL + "?cmd=diary_del", params, func);
		}
		mDiaryManager.getDate = function (id, func) {
			var params =
			{
				"id": id
			};
			AFL.sendJson(MANAGER_URL + "?cmd=diary_date", params, func);
		}
		mDiaryManager.getLastDate = function (func) {
			var params =
			{
			};
			AFL.sendJson(MANAGER_URL + "?cmd=diary_date_last", params, func);
		}
		function dateToStr(date)
		{
			return AFL.sprintf("%d/%d/%d", date.getFullYear(), date.getMonth() + 1, date.getDate());
		}
		mDiaryManager.searchDiary = function (keyword, func) {
			var params =
			{
				"keyword": keyword
			};
			AFL.sendJson(MANAGER_URL + "?cmd=diary_search", params, func);
		}
		mDiaryManager.getDiary = function (id, dateStart, dateEnd, func) {
			var params =
			{
				"id": id,
				"count": null
			};
			if (dateStart)
				params["date"] = dateToStr(dateStart);
			if (dateEnd)
				params["dateEnd"] = dateToStr(dateEnd);
			AFL.sendJson(MANAGER_URL + "?cmd=diary_get", params, onLoad);
			function onLoad(params)
			{
				if (params == null)
					func(null);
				else
				{
					var datas = params["diary"];
					for (var index in datas) {
						var diary = datas[index];
						var node = document.createElement("div");
						node.innerHTML = diary["diaryMessage"];
						mDiaryManager.convertDiaryId2(node);
						diary["diaryMessage"] = node.innerHTML;
					}
					func(params);
				}

			}
		}
		var mFileManager = AFL.createFileManager(ServletURL);

		mDiaryManager.convertDiaryId2 = function(node) {
			if (node.tagName == null)
				return;
			var tagName = node.tagName.toLowerCase();
			if (tagName == "a") {
				if (node.dataset && node.dataset.id != null)
					node.href = mFileManager.getUrl(node.dataset.id);
			}
			else if (tagName == "img" || tagName == "image") {
				if (node.dataset && node.dataset.id != null)
					node.src = mFileManager.getUrl(node.dataset.id);
			}
			for (var index in node.childNodes) {
				this.convertDiaryId2(node.childNodes[index]);
			}
		}

		return mDiaryManager;
	}



	function convertDiaryId(node) {
		if (node.tagName == null)
			return;
		var tagName = node.tagName.toLowerCase();
		if (tagName == "a") {
			if (node.dataset.id != null)
				node.href = "";
		}
		else if (tagName == "img" || tagName == "image") {
			if (node.dataset.id != null)
				node.src = "";
		}
		for (var index in node.childNodes) {
			convertDiaryId(node.childNodes[index]);
		}
	}


	WM.createEditFrame = function(ServletURL) {
		var mMessageId = 0;
		mDiaryManager = AFL.createDiaryManager(ServletURL);

		var mEditFrame = WM.createFrameWindow();
		mEditFrame.setTitle("編集");

		var mPanel = WM.createPanel();
		mPanel.setClientStyle(WM.STYLE_TOP);
		mEditFrame.addChild(mPanel);

		var mButtonSet = WM.createButton();
		mButtonSet.setText("保存");
		mButtonSet.setClientStyle(WM.STYLE_LEFT);
		mPanel.addChild(mButtonSet);

		var mMessage = null;
		mButtonSet.onclick = function () {
			function onWrite(param) {
				if (param != null) {
					mMessageId = param["id"];
					mMessage.close();
					if (mMessageId == 1)
					    System.drawInfo();
                    else
    					System.drawDiaryById(mMessageId);
				}
				else {
					mMessage.setText("書き込みエラー");
				}
			}

			mMessage = WM.createMessageView("書き込み中");
			var nodeMsg = mMessageView.getHtmlNode();
			convertDiaryId(nodeMsg);

			mDiaryManager.writeDiary(mMessageId, mTitleView.getValue(), nodeMsg.innerHTML, mDate, mButtonVisible.isCheck(), onWrite);
		}


		var mButtonDate = WM.createButton();
		mButtonDate.setText("日付");
		mButtonDate.setClientStyle(WM.STYLE_LEFT);
		mButtonDate.setSize(140, 16);
		mPanel.addChild(mButtonDate);

		mButtonDate.onclick = function () {
			var frame = WM.createFrameWindow();
			var cal = WM.createCalendarView();
			cal.clearDateColor();
			cal.setDateColor(mDate,0xdd448844);
			cal.setDate(mDate);
			cal.addEvent("onDay", function (params) {
			    var date = params["date"];
				cal.clearDateColor();
				cal.setDateColor(date, 0xdd448844);
				mEditFrame.setDate(date);
			});
			frame.addChild(cal);

			frame.setClientSize(cal.getWidth(), cal.getHeight());
			frame.setPos();
			frame.setTopMost(true);
		}

		var mButtonVisible = WM.createButton("checkbox");
		mButtonVisible.setClientStyle(WM.STYLE_LEFT);
		mPanel.addChild(mButtonVisible);
		mButtonVisible.setCheck(true);


		var mDate = null;
		mEditFrame.setDate = function (date) {
			mDate = date;
			mButtonDate.setText(AFL.sprintf("%d年%d月%d日", date.getFullYear(), date.getMonth() + 1, date.getDate()));
		}


		var mButtonDel = WM.createButton();
		mButtonDel.setText("削除");
		mButtonDel.setClientStyle(WM.STYLE_RIGHT);
		mPanel.addChild(mButtonDel);

		var mTitleView = WM.createTextBox();
		mTitleView.setBackgroundColor(0xe0e0e0e0);
		mTitleView.setClientStyle(WM.STYLE_TOP);
		mEditFrame.addChild(mTitleView);

		var mMessageView = WM.createHtmlEditView();
		mEditFrame.addChild(mMessageView);
		mMessageView.getFile = function () {
			var file = WM.createFileView(ServletURL);
			var dir = AFL.sprintf("/diary/%04d/%02d/%02d", mDate.getFullYear(), mDate.getMonth() + 1, mDate.getDate());
			file.mkdir(dir, function (param) { file.cd(param["id"]); });

			return file;
		}

		mEditFrame.setDate(new Date());

		mEditFrame.load = function(id)
		{
		    var messageView = WM.createMessageView("編集データの読み出し");
			function onLoad(params)
		    {
			    flag = true;
			    if (params != null)
			    {
			        var diarys = params["diary"];
			        if (diarys && diarys.length != 0) {
			            var diary = diarys[0];
			            mMessageId = diary["diaryId"];
			            mTitleView.setText(diary["diaryTitle"]);
			            mButtonVisible.setCheck(diary["diaryVisible"]);
			            mMessageView.setValue(diary["diaryMessage"]);
			            mEditFrame.setDate(new Date(diary["diaryDate"]));
			            flag = false;
			            messageView.close();
			        }else if (id == 1) {
			            mMessageId = 1;
			            flag = false;
			            messageView.close();
			        }
			    }

			    if (flag)
			        messageView.setText("データ読み出しエラー");
			}
			mDiaryManager.getDiary(parseInt(id),null,null, onLoad);
		}

		mEditFrame.setPos();
		return mEditFrame;
	}
})();
