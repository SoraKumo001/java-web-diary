//ファイルマネージャ

(function () {
	AFL.createFileManager = function(ServletURL)
	{
		var MANAGER_URL = ServletURL + "FileManager"
		var manager = {};
		manager.moveFile = function(src, dest,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=file_move", 
				{ "src_id": src, "dest_id": dest }, func);
		}
		manager.getList = function(id,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=files", { "id": id }, func);
		}
		manager.createDir = function(pid,name,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=dir_new",
				{ "pid": pid, "name": name }, func);
		}
		manager.renameFile = function(id,name,func)
		{
			var param = { "id": id, "name": name };
			AFL.sendJson(MANAGER_URL + "?cmd=file_rename", param, func);
		}
		manager.delFile = function(id,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=file_del", { "id": id }, func);
		}
		manager.getDir = function(id,func)
		{
			AFL.sendJson(MANAGER_URL + "?cmd=dir", { "id": id }, func);
		}
		manager.getFileId = function (pid,name, func) {
			AFL.sendJson(MANAGER_URL + "?cmd=file_id", { "pid": pid,"name":name }, func);
		}
		manager.upload = function(pid,name,data,func)
		{
			AFL.sendFile(MANAGER_URL, { "cmd": "file_upload", "pid": pid, "filename": name }, data, func);
		}
		manager.download = function (id) {
			window.location = MANAGER_URL+"?cmd=file_download&id="+id;
		}
		manager.getUrl = function (id) {
			return MANAGER_URL + "?cmd=file_download&id=" + id;
		}
		return manager;
	}

	//フォルダーアイコン(暫定)
	function createFolderIcon() {
		var canvas = document.createElement("canvas");
		canvas.width = 16;
		canvas.height = 16;
		var ctx = canvas.getContext('2d');
		ctx.beginPath();
		ctx.fillStyle = "rgb(255, 215, 0)";
		ctx.fillRect(1, 1, 14, 14);
		var url = canvas.toDataURL();
		var img = document.createElement("img");
		img.src = url;
		return img;
	}
	//ファイルアイコン(暫定)
	function createFileIcon() {
		var canvas = document.createElement("canvas");
		canvas.width = 16;
		canvas.height = 16;
		var ctx = canvas.getContext('2d');
		ctx.beginPath();
		ctx.fillStyle = "rgb(230, 240, 250)";
		ctx.fillRect(1, 1, 14, 14);
		var url = canvas.toDataURL();
		var img = document.createElement("img");
		img.src = url;
		return img;
	}
	//ファイル管理用ビューの作成
	WM.createFileView = function (ServletURL)
	{
		var mManager = AFL.createFileManager(ServletURL);

		//アイコン作成
		var mFolderIcon = createFolderIcon();
		var mFileIcon = createFileIcon();
		//フレームウインドウ初期設定
		var frame = WM.createFrameWindow();
		frame.setTitle("ファイルマネージャ");
		frame.setBackgroundColor(0xeeeeeeff);
		frame.setSize(640, 400);
		frame.style.fontFamily = "Courier";
		//ウインドウ分割
		var split = WM.createSplit();
		split.setClientStyle(WM.STYLE_CLIENT);
		split.setBarPos(180);
		frame.addChild(split);

		//--------------------------------------------
		//ツリービュー作成
		var mTreeView = WM.createTreeView();
		mTreeView.setClientStyle(WM.STYLE_CLIENT);
		split.addChild(mTreeView, 0);
		mTreeView.setTargetMode(true);

		//ドラッグドロップによる移動処理
		mTreeView.addEvent("onItemDrop",
			function (item) {
				var src = item.getItemValue();
				var hoverItem = mTreeView.getHoverItem();
				var dest = hoverItem ? hoverItem.getItemValue() : -1;
				mManager.moveFile(src, dest, function () { frame.reload(); });
			});
		//アイテム選択による処理
		mTreeView.addEvent("onItemSelect",
			function (item) {
				var id = item.getItemValue();
				mSelectDirId = id;
				mManager.getList(id, onFileLoad);
			});
		function isImage(name)
		{
			var p = name.lastIndexOf(".");
			if (p == -1)
				return false;
			var c = name.substr(p + 1).toLowerCase();
			if (c == "jpeg" || c == "jpg" || c == "gif" || c == "png")
				return true;
			return false;
		}
		//ファイルリスト更新処理
		function onFileLoad(datas) {
			list.clear();
			if (datas == null)
				return;
			var files = datas.files;
			for (var index in files) {
				var file = files[index];
				var item;
				if (file.kind == 0)
					item = list.addItem(mFolderIcon.cloneNode());
				else
				{
					if (isImage(file.name)) {
						var img = document.createElement("img");
						img.width = 16;
						img.height = 16;
						img.src = mManager.getUrl(file.id);
						item = list.addItem(img);
					} else
						item = list.addItem(mFileIcon.cloneNode());
				}
				list.setItemValue(item, file);
				list.setItem(item, 1, file.name);
				list.setItem(item, 2, file.size);

				var d = new Date();
				d.setTime(parseInt(file.date));
				var dateStr = AFL.sprintf("%04d-%02d-%02d %02d:%02d:%02d",
						d.getFullYear(), d.getMonth() + 1, d.getDate(),
						d.getHours(), d.getMinutes(), d.getSeconds());
				list.setItem(item, 3, dateStr);
			}
			list.sort();
			list.redraw();
		}
		//--------------------------------------------

		//--------------------------------------------
		//リストビューの作成
		var list = WM.createListView();
		list.setClientStyle(WM.STYLE_CLIENT);
		split.addChild(list, 1);
		list.setTargetMode(true);
		list.sort(1, true);

		function onSortName(a,b)
		{
			var sortFlag = list.isSortFlag(1)?1:-1;
			var kindA = list.getItemValue(a).kind;
			var kindB = list.getItemValue(b).kind;
			if (kindA < kindB)
				return -1 * sortFlag;
			if (kindA > kindB)
				return 1 * sortFlag;
			var valueA = list.getItemText(a, 1);
			var valueB = list.getItemText(b, 1);
			if (valueA < valueB)
				return -1 * sortFlag;
			if (valueA == valueB)
				return 0;
			return sortFlag;
		}
		list.setSortFunc(1, onSortName);

		list.addHeader("*");
		list.setHeaderWidth(0, 20);
		list.addHeader("名前");
		list.setHeaderWidth(1, 160);
		list.addHeader("サイズ");
		list.addHeader("日時");
		list.setHeaderWidth(3, 160);
		list.ondragover = function (e) {
			e.preventDefault();
		}
		list.ondrop = function (e) {
			e.preventDefault();

			var files = e.dataTransfer.files;
			for (var i = 0; i < files.length; i++) {

				var item = mTreeView.getSelectItem();
				id = item.getItemValue();

				var win = WM.createFrameWindow();
				frame.addChild(win);
				win.setTopMost(true);
				win.setBackgroundColor(0xffffffff);
				win.setSize(128, 64);
				win.setPos();
				win.setTitle("転送状況");
				win.setHtml("転送開始");

				var reader = new FileReader();
				reader.file = files[i];
				reader.pid = id;
				reader.onload = function (evt) {

					mManager.upload(this.pid, this.file.name, evt.target.result, 
						function (size) {
							if (size == -1) {
								win.setHtml("転送エラー");
							}
							else if (size == reader.file.size) {
								win.close();
								frame.reload();
							}
							else {
								win.setHtml(size + "/" + reader.file.size);
							}
						});
				}
				reader.readAsArrayBuffer(reader.file);
			}
		}
		var mSelectDirId = 0;
		function onDirLoad(datas) {
			function add(item, childs) {
				if (childs == null)
					return;
				var child;

				for (var i = 0; child = childs[i]; i++) {
					var childItem = item.addItem(child["name"]);
					childItem.setItemValue(child["id"]);
					add(childItem, child.childs);
				}
			}
			var root = datas["dir"];
			var expands = mTreeView.saveExpand();
			mTreeView.clear();

			var itemRoot = mTreeView.addItem("[ROOT]", true);
			itemRoot.setItemValue(1);
			add(itemRoot, root);
			mTreeView.loadExpand(expands);

			frame.selectDir(mSelectDirId);
		}
		list.addEvent("onItemDblClick", onItemDblClick);
		function onItemDblClick(e)
		{
			var file = list.getItemValue(e.itemIndex);
			var kind = file.kind;
			var name = file.name;
			var id = file.id;
			if (kind != 0) {
				if (frame.onExec)
					frame.onExec(id,name, mManager.getUrl(id));
				else
					mManager.download(id);
			}
			else {
				frame.selectDir(id);
			}

		}
		//ドラッグドロップによる移動処理
		list.addEvent("onItemDrop",
			function (index) {
				var src = list.getItemValue(index).id;
				var hoverItem = mTreeView.getHoverItem();
				var dest = hoverItem ? hoverItem.getItemValue() : -1;
				mManager.moveFile(src, dest, function () { frame.reload(); });
			});
		//--------------------------------------------

		//--------------------------------------------
		//コマンドパネルの作成
		var panel = WM.createPanel();
		panel.setClientStyle(WM.STYLE_BOTTOM);
		frame.addChild(panel);

		//入力ボックス作成
		function createInput() {
			var box = WM.createInputBox();
			box.setTopMost(true);
			frame.addChild(box);
			box.setPos();
			box.setFocus();
			return box;
		}

		var button;
		button = WM.createButton();
		button.setText("新規");
		button.setSize(60);
		button.setClientStyle(WM.STYLE_LEFT);
		panel.addChild(button);
		button.onclick = function () {
			var item = mTreeView.getSelectItem();
			if (!item)
				return;
			var box = createInput();
			box.setTitle("New dir");
			box.onEnter = function () {
				var name = box.getText();
				if (name) {
					mManager.createDir(item.getItemValue(), box.getText(), function () { frame.reload(); });
					box.close();
				}
			}
		}


		button = WM.createButton();
		button.setText("変更");
		button.setSize(60);
		button.setClientStyle(WM.STYLE_LEFT);
		panel.addChild(button);
		button.onclick = function () {
			var box = createInput();
			var name;
			var id;
			if (WM.getTarget() == mTreeView)
			{
				var item = mTreeView.getSelectItem();
				id = item.getItemValue();
				name = item.getItemText();
			}
			else
			{
				var item = list.getSelectIndex();
				if (item == -1)
					return;
				id = list.getItemValue(item).id;
				name = list.getItemText(item,1);
			}
			box.setText(name);
			box.setTitle("Rename dir");
			box.onEnter = function () {
				var name = box.getText();
				if (name) {
					mManager.renameFile(id, box.getText(), function () { frame.reload(); });
					box.close();
				}

			}

		}


		button = WM.createButton();
		button.setClientStyle(WM.STYLE_LEFT);
		button.setText("削除");
		button.setSize(60);
		panel.addChild(button);
		button.onclick = function () {
			var id;
			if (WM.getTarget() == mTreeView) {
				var item = mTreeView.getSelectItem();
				id = item.getItemValue();
				mManager.delFile(id, function () { frame.reload(); });
			}
			else {
				var items = list.getSelectIndexes();
				for (var index in items)
				{
					id = list.getItemValue(items[index]).id;
					mManager.delFile(id, function () { frame.reload(); });
				}
			}

		}
		//--------------------------------------------
		frame.mkdir = function(dir,func)
		{
			mManager.createDir(0, dir,
				function (param) { mSelectDirId = param["id"]; frame.reload(); });
		}
		frame.selectDir = function(id)
		{
			var item = mTreeView.findItem(id);
			if (item != null)
				mTreeView.selectItem(item);
			else {
				item = mTreeView.findItem(1);
				if (item != null)
					mTreeView.selectItem(item);
			}
		}
		frame.cd = function(id)
		{
			frame.selectDir(id);
		}
		frame.reload = function () {
			mManager.getDir(1, onDirLoad);
		}
		frame.reload();
		return frame;
	}
})();