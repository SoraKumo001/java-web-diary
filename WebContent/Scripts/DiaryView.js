(function () {
	WM.createDiaryView = function(ServletURL) {
		var mDiaryManager = AFL.createDiaryManager(ServletURL);
		var mDiaryView = WM.createWindow();
		mDiaryView.setScroll(true);
		mDiaryView.setClientStyle(WM.STYLE_CLIENT);

		function createDiaryDay(date) {
			var TEMPLATE =
				"<DIV style='background-color:#e8e8ff;margin:0.5em;padding:0.1em;'>" +
				"<A id='link' style='text-decoration: none;'><DIV id='date' style='background-color:#44aa44;color:#FFFF33;font-weight:bold;font-size:1.5em;margin:0.3em;padding:0.3em;'></DIV></A>" +
				"<DIV id='diary' ></DIV>" +
				"</DIV>";
			var div = document.createElement("div");
			div.innerHTML = TEMPLATE;
			var mDate = div.querySelector("#date");
			var mDiary = div.querySelector("#diary");
			var mLink = div.querySelector("#link");

			var d = new Date(date);
			var linkName = AFL.sprintf("%d-%d-%d", d.getFullYear(), d.getMonth() + 1, d.getDate());
			mLink.href = "?p=" + linkName;
			mLink.name = linkName;
			div.id = "A" + linkName;

			div.setDate = function (value) {
				var date = new Date(value);
				mDate.textContent = AFL.sprintf("%d年%d月%d日", date.getFullYear(), date.getMonth() + 1, date.getDate());
			}
			div.addDiary = function (value) {
				mDiary.appendChild(value);
			}
			div.setDate(date);
			return div;
		}
		function createDiary(diary) {
			var TEMPLATE_DIARY =
				"<DIV id='block' style='background-color:#f5f5ff;margin:0.5em;padding:0.6em;'>" +
				"<DIV id='edit' style='float:right;margin:0.5em;cursor:pointer'>編集</DIV>" +
				"<DIV id='del' style='float:right;margin:0.5em;cursor:pointer'>削除</DIV>" +
				"<A id='link' style='font-size:1.7em;text-decoration:none'>" +
				 "<DIV  style='float:left;color:#FAFA44;'>★</DIV>" +
				 "<DIV id='title' style='color:#FF6677;font-weight:padding:0.2em;'></DIV>" +
				"</A>" +
				"<DIV id='message' style='padding:0.8em;padding-left:2em;clear:both;'></DIV>" +
				"</DIV>";
			var div = document.createElement("div");
			div.innerHTML = TEMPLATE_DIARY;
			var mBlock = div.querySelector("#block");
			var mDel = div.querySelector("#del");
			var mLink = div.querySelector("#link");
			var mEdit = div.querySelector("#edit");
			var mTitle = div.querySelector("#title");
			var mMessage = div.querySelector("#message");
			div.setId = function (value) {
				mLink.name = value;
				mLink.href = "?p=" + value;
				div.id = "A"+value;
			}
			div.setTitle = function (value) {
				mTitle.textContent = value;
			}
			div.setMessage = function (value) {
				mMessage.innerHTML = value;
				var nodes = mMessage.getElementsByTagName("a");
				for(var index in nodes)
				{
					var node = nodes[index];
					if(node.href)
					{
						node._href = node.href;
						node.onclick = function () { location = this._href; return false; }
						node.href = "javascript:void(0);";
					}
				}
				//ソースコードのハイライト
				hljs.configure({ useBR: true });
				var nodes = mMessage.querySelectorAll(".code");
				for (var index = 0; nodes[index];index++)
				{
					var node = nodes[index];
					node.className = "";
					node.style.padding = "3px";
					node.style.maxWidth = "100%";
					node.style.display = "inline-block";
					node.style.whiteSpace = "nowrap";
					node.style.overflow = "auto";
					
                    hljs.highlightBlock(node);
				}
			}
			mEdit.onclick = function () {
				System.editDiary(mLink.name);
			}
			mDel.onclick = function()
			{
				var mMessageView = WM.createMessageView("削除中");
				function onDel()
				{
					mMessageView.close();
					System.loadDiary();
				}
				mDiaryManager.delDiary(parseInt(mLink.name),onDel);
			}
			if (!System.isAdmin())
			{
				mBlock.removeChild(mEdit);
				mBlock.removeChild(mDel);
			}
			
			if (!diary["diaryVisible"])
				mBlock.style.backgroundColor = WM.getARGB(0xffBBBBBB);
			div.setId(diary["diaryId"]);
			div.setTitle(diary["diaryTitle"]);
			div.setMessage(diary["diaryMessage"]);

			return div;
		}


		mDiaryView.drawDiary = function (params) {
			mDiaryView.innerHTML = "";

			//並び替え
			var listDiary = params["diary"];
			listDiary.sort(function (a, b) {
				if (a["diaryDate"] > b["diaryDate"])
					return -1;
				if (a["diaryDate"] < b["diaryDate"])
					return 1;
				return a["diaryPriority"] - b["diaryPriority"];
			});
			var date = null;
			var areaDay;
			for (var index in listDiary) {
				var diary = listDiary[index];
				if (diary["enable"])
				{
					if (date != diary["diaryDate"]) {
						date = diary["diaryDate"];
						areaDay = createDiaryDay(diary["diaryDate"]);
						mDiaryView.setHtml(areaDay);
					}
					var area = createDiary(diary);
					areaDay.addDiary(area);
				}
			}
		    //表示位置の再設定
            
			if (window.location.search.length > 3) {
			    setTimeout(function () {
			        var block = mDiaryView.querySelector("#A" + window.location.search.substring(3));
			        if (block) {
			            mDiaryView.scrollTop = block.offsetTop - 50;
			        }
			    }, 10);
			}
		}
		return mDiaryView;
	}

})();